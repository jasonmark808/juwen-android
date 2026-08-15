package cc.juwen.reader.data

import cc.juwen.reader.model.FeedSnapshot
import cc.juwen.reader.model.NewsSource
import cc.juwen.reader.model.Story
import org.json.JSONObject

object FeedParser {
    fun parse(raw: String): FeedSnapshot {
        val root = JSONObject(raw)
        val schema = root.getInt("schema_version")
        require(schema == 1) { "Unsupported schema: $schema" }
        val generatedAt = root.getString("generated_at")
        val mode = root.optString("mode", "rules")
        val items = root.getJSONArray("stories")
        require(items.length() <= 2_000) { "Feed is too large" }

        val stories = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val sourcesJson = item.getJSONArray("sources")
                val sources = buildList {
                    for (sourceIndex in 0 until sourcesJson.length()) {
                        val source = sourcesJson.getJSONObject(sourceIndex)
                        val url = source.getString("url")
                        require(url.startsWith("https://")) { "Only HTTPS links are accepted" }
                        add(NewsSource(source.getString("name"), url, source.optInt("rank", 0)))
                    }
                }
                add(
                    Story(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        category = item.optString("category", "综合"),
                        publishedAt = item.optString("published_at", generatedAt),
                        collectedAt = item.optString("collected_at", generatedAt),
                        score = item.optDouble("score", 0.0),
                        important = item.optBoolean("important", false),
                        summary = item.optString("summary").ifBlank { null },
                        sources = sources,
                    )
                )
            }
        }
        return FeedSnapshot(
            schemaVersion = schema,
            generatedAt = generatedAt,
            mode = mode,
            stories = stories.distinctBy { it.id },
            collectionStatus = root.optString("collection_status", "complete"),
            successfulSourceCount = root.optInt("successful_source_count", 0),
            failedSourceCount = root.optInt("failed_source_count", 0),
        )
    }
}
