#!/usr/bin/env python3
"""Build the public JuWen feed with deterministic, AI-free rules."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import urllib.parse
import urllib.request
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SOURCES = {
    "toutiao": "今日头条",
    "baidu": "百度热搜",
    "wallstreetcn-hot": "华尔街见闻",
    "thepaper": "澎湃新闻",
    "bilibili-hot-search": "哔哩哔哩",
    "cls-hot": "财联社",
    "ifeng": "凤凰网",
    "tieba": "百度贴吧",
    "weibo": "微博",
    "douyin": "抖音",
    "zhihu": "知乎",
}

CATEGORIES = {
    "财经": ("股市", "基金", "央行", "金融", "经济", "公司", "财报", "楼市", "人民币"),
    "科技": ("AI", "人工智能", "芯片", "模型", "机器人", "互联网", "科技", "苹果", "华为"),
    "时政": ("政策", "国务院", "外交", "会议", "部长", "法规", "政府", "两会"),
    "社会": ("警方", "教育", "医院", "天气", "民生", "事故", "城市", "学校"),
    "汽车": ("汽车", "新能源车", "电动车", "比亚迪", "小米汽车", "特斯拉", "车企"),
    "数码": ("手机", "平板", "电脑", "相机", "耳机", "发布会", "数码"),
}


@dataclass(frozen=True)
class RawItem:
    title: str
    url: str
    source_id: str
    source_name: str
    rank: int
    published_at: str


def normalize_title(title: str) -> str:
    return re.sub(r"[^0-9a-zA-Z\u4e00-\u9fff]", "", title).lower()


def category_for(title: str) -> str:
    folded = title.casefold()
    scores = {name: sum(1 for word in words if word.casefold() in folded) for name, words in CATEGORIES.items()}
    best = max(scores, key=scores.get)
    return best if scores[best] else "综合"


def is_https(url: str) -> bool:
    parsed = urllib.parse.urlparse(url)
    return parsed.scheme == "https" and bool(parsed.netloc)


def extract_items(payload: Any, source_id: str) -> list[RawItem]:
    if isinstance(payload, dict):
        candidates = payload.get("items") or payload.get("data") or payload.get("list") or []
        if isinstance(candidates, dict):
            candidates = candidates.get("items") or candidates.get("data") or []
    elif isinstance(payload, list):
        candidates = payload
    else:
        candidates = []

    result: list[RawItem] = []
    now = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    for index, item in enumerate(candidates):
        if not isinstance(item, dict):
            continue
        title = str(item.get("title") or item.get("name") or "").strip()
        url = str(item.get("url") or item.get("link") or item.get("mobileUrl") or "").strip()
        if not title or not is_https(url):
            continue
        result.append(
            RawItem(
                title=title[:240],
                url=url,
                source_id=source_id,
                source_name=SOURCES.get(source_id, source_id),
                rank=int(item.get("rank") or item.get("position") or index + 1),
                published_at=str(item.get("published_at") or item.get("time") or now),
            )
        )
    return result


def fetch_sources(api_bases: list[str]) -> tuple[list[RawItem], list[dict[str, Any]]]:
    all_items: list[RawItem] = []
    diagnostics: list[dict[str, Any]] = []
    for source_id in SOURCES:
        last_error: Exception | None = None
        collected: list[RawItem] = []
        for api_base in api_bases:
            separator = "&" if "?" in api_base else "?"
            url = f"{api_base}{separator}id={urllib.parse.quote(source_id)}"
            request = urllib.request.Request(
                url,
                headers={
                    "Accept": "application/json",
                    "User-Agent": "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 JuWen/1.0",
                    "Referer": "https://newsnow.busiyi.world/",
                },
            )
            try:
                with urllib.request.urlopen(request, timeout=20) as response:
                    payload = json.loads(response.read().decode("utf-8"))
                collected = extract_items(payload, source_id)
                if collected:
                    break
                last_error = RuntimeError("endpoint returned no valid HTTPS items")
            except Exception as error:
                last_error = error
        if collected:
            all_items.extend(collected)
            diagnostics.append({"id": source_id, "name": SOURCES[source_id], "status": "ok", "item_count": len(collected)})
        else:
            message = str(last_error or "no endpoint succeeded")[:240]
            diagnostics.append({"id": source_id, "name": SOURCES[source_id], "status": "failed", "item_count": 0, "error": message})
            print(f"warning: {source_id}: {message}", file=sys.stderr)
    if not all_items:
        raise RuntimeError("No valid HTTPS headlines were collected")
    return all_items, diagnostics


def load_fixture(path: Path) -> list[RawItem]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    result: list[RawItem] = []
    for source_id, source_payload in payload.items():
        result.extend(extract_items(source_payload, source_id))
    return result


def build_snapshot(
    items: list[RawItem],
    generated_at: str | None = None,
    diagnostics: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    timestamp = generated_at or datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    groups: dict[str, list[RawItem]] = defaultdict(list)
    for item in items:
        key = normalize_title(item.title)
        if len(key) >= 4:
            groups[key].append(item)

    stories = []
    for key, grouped in groups.items():
        unique_sources = {item.source_id: item for item in sorted(grouped, key=lambda entry: entry.rank)}
        sources = list(unique_sources.values())
        best_rank = min(item.rank for item in sources)
        cross_platform = len(sources)
        score = round(100 / (best_rank + 4) + cross_platform * 12 + len(grouped) * 2, 2)
        story_id = hashlib.sha256(key.encode("utf-8")).hexdigest()[:20]
        representative = min(sources, key=lambda entry: entry.rank)
        stories.append(
            {
                "id": story_id,
                "title": representative.title,
                "category": category_for(representative.title),
                "published_at": representative.published_at,
                "collected_at": timestamp,
                "score": score,
                "important": cross_platform >= 2 or best_rank <= 3,
                "summary": None,
                "sources": [
                    {"name": item.source_name, "url": item.url, "rank": item.rank}
                    for item in sorted(sources, key=lambda entry: entry.rank)
                ],
            }
        )
    stories.sort(key=lambda story: (-story["score"], story["title"]))
    health = diagnostics or []
    successful = sum(item.get("status") == "ok" for item in health)
    failed = sum(item.get("status") == "failed" for item in health)
    return {
        "schema_version": 1,
        "generated_at": timestamp,
        "mode": "rules",
        "collection_status": "partial" if failed else "complete",
        "successful_source_count": successful,
        "failed_source_count": failed,
        "source_diagnostics": health,
        "stories": stories[:1000],
    }


def validate_snapshot(snapshot: dict[str, Any], minimum_stories: int = 10) -> None:
    stories = snapshot.get("stories", [])
    if len(stories) < minimum_stories:
        raise RuntimeError(f"Refusing to publish {len(stories)} stories; minimum is {minimum_stories}")
    if snapshot.get("successful_source_count", 0) < 1:
        raise RuntimeError("Refusing to publish without a successful source")


def write_outputs(snapshot: dict[str, Any], output: Path) -> None:
    feeds = output / "feeds"
    feeds.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(snapshot, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    (feeds / "latest.json").write_text(serialized, encoding="utf-8")
    categories = sorted({story["category"] for story in snapshot["stories"]})
    for category in categories:
        subset = {**snapshot, "stories": [story for story in snapshot["stories"] if story["category"] == category]}
        safe_name = hashlib.sha1(category.encode("utf-8")).hexdigest()[:10]
        (feeds / f"category-{safe_name}.json").write_text(json.dumps(subset, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    digest = hashlib.sha256(serialized.encode("utf-8")).hexdigest()
    manifest = {
        "schema_version": 1,
        "generated_at": snapshot["generated_at"],
        "mode": snapshot["mode"],
        "story_count": len(snapshot["stories"]),
        "categories": categories,
        "latest_sha256": digest,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-base", help="Comma-separated NewsNow-compatible /api/s endpoints")
    parser.add_argument("--fixture", type=Path, help="Offline source fixture")
    parser.add_argument("--output", type=Path, default=Path("site"))
    args = parser.parse_args()
    if bool(args.api_base) == bool(args.fixture):
        parser.error("provide exactly one of --api-base or --fixture")
    if args.api_base:
        endpoints = [item.strip().rstrip("/") for item in args.api_base.split(",") if item.strip()]
        items, diagnostics = fetch_sources(endpoints)
        snapshot = build_snapshot(items, diagnostics=diagnostics)
        validate_snapshot(snapshot)
    else:
        items = load_fixture(args.fixture)
        fixture_health = [{"id": source_id, "name": SOURCES.get(source_id, source_id), "status": "ok", "item_count": len(extract_items(payload, source_id))} for source_id, payload in json.loads(args.fixture.read_text(encoding="utf-8")).items()]
        snapshot = build_snapshot(items, diagnostics=fixture_health)
    write_outputs(snapshot, args.output)


if __name__ == "__main__":
    main()
