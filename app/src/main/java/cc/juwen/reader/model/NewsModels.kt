package cc.juwen.reader.model

data class NewsSource(
    val name: String,
    val url: String,
    val rank: Int,
)

data class Story(
    val id: String,
    val title: String,
    val category: String,
    val publishedAt: String,
    val collectedAt: String,
    val score: Double,
    val important: Boolean,
    val summary: String?,
    val sources: List<NewsSource>,
)

data class FeedSnapshot(
    val schemaVersion: Int,
    val generatedAt: String,
    val mode: String,
    val stories: List<Story>,
    val collectionStatus: String = "complete",
    val successfulSourceCount: Int = 0,
    val failedSourceCount: Int = 0,
)
