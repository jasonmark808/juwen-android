import json
import tempfile
import unittest
from pathlib import Path

from pipeline.build_feed import build_snapshot, category_for, extract_items, load_fixture, normalize_title, validate_snapshot, write_outputs


FIXTURE = Path(__file__).parent / "fixtures" / "sources.json"


class BuildFeedTest(unittest.TestCase):
    def test_normalization_and_category(self):
        self.assertEqual(normalize_title("AI：芯片！"), "ai芯片")
        self.assertEqual(category_for("国产AI芯片发布新进展"), "科技")

    def test_rejects_non_https(self):
        payload = [{"title": "bad", "url": "http://example.com", "rank": 1}]
        self.assertEqual(extract_items(payload, "weibo"), [])

    def test_cross_platform_story_is_important(self):
        items = load_fixture(FIXTURE)
        snapshot = build_snapshot(items, "2026-08-15T08:00:00Z")
        story = next(item for item in snapshot["stories"] if item["title"] == "国产AI芯片发布新进展")
        self.assertTrue(story["important"])
        self.assertEqual(len(story["sources"]), 2)
        self.assertEqual(snapshot["mode"], "rules")

    def test_outputs_are_versioned_and_stable(self):
        snapshot = build_snapshot(load_fixture(FIXTURE), "2026-08-15T08:00:00Z")
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            write_outputs(snapshot, output)
            manifest = json.loads((output / "manifest.json").read_text(encoding="utf-8"))
            feed = json.loads((output / "feeds" / "latest.json").read_text(encoding="utf-8"))
            self.assertEqual(manifest["schema_version"], 1)
            self.assertEqual(manifest["story_count"], len(feed["stories"]))
            self.assertEqual(feed["generated_at"], "2026-08-15T08:00:00Z")

    def test_collection_health_reports_partial_sources(self):
        diagnostics = [
            {"id": "toutiao", "name": "今日头条", "status": "ok", "item_count": 20},
            {"id": "weibo", "name": "微博", "status": "failed", "item_count": 0, "error": "timeout"},
        ]
        snapshot = build_snapshot(load_fixture(FIXTURE), "2026-08-15T08:00:00Z", diagnostics)
        self.assertEqual(snapshot["collection_status"], "partial")
        self.assertEqual(snapshot["successful_source_count"], 1)
        self.assertEqual(snapshot["failed_source_count"], 1)

    def test_refuses_to_publish_too_few_stories(self):
        snapshot = build_snapshot(load_fixture(FIXTURE), diagnostics=[{"status": "ok"}])
        with self.assertRaisesRegex(RuntimeError, "minimum is 10"):
            validate_snapshot(snapshot)


if __name__ == "__main__":
    unittest.main()
