package cc.juwen.reader.data

import android.content.Context
import cc.juwen.reader.BuildConfig
import cc.juwen.reader.model.FeedSnapshot
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.time.Instant

class FeedRepository(private val context: Context) {
    private val cache = File(context.filesDir, "latest-feed.json")
    private val preferences = context.getSharedPreferences("juwen", Context.MODE_PRIVATE)

    fun loadCached(): FeedSnapshot? = runCatching {
        if (cache.exists()) FeedParser.parse(cache.readText())
        else if (BuildConfig.E2E_FIXTURE) context.assets.open("e2e-feed.json").bufferedReader().use { FeedParser.parse(it.readText()) }
        else context.assets.open("baseline-feed.json").bufferedReader().use { FeedParser.parse(it.readText()) }
    }.getOrNull()

    fun refresh(): Result<FeedSnapshot> = runCatching {
        val configured = preferences.getString("feed_url", null)?.trim().orEmpty()
        val primary = configured.ifBlank { BuildConfig.DEFAULT_FEED_URL }
        val candidates = listOf(primary, cdnFallback(primary)).distinct()
        var lastError: Throwable? = null
        for (candidate in candidates) {
            try {
                val raw = download(candidate)
                val snapshot = FeedParser.parse(raw)
                persistAtomically(raw)
                preferences.edit()
                    .putString("last_sync", snapshot.generatedAt)
                    .putString("last_checked", Instant.now().toString())
                    .apply()
                return@runCatching snapshot
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("No feed endpoint configured")
    }

    fun setFeedUrl(value: String) {
        require(isValidFeedUrl(value)) { "Feed endpoint must use HTTPS" }
        preferences.edit().putString("feed_url", value.trim()).apply()
    }

    fun testFeedUrl(value: String): Result<FeedSnapshot> = runCatching {
        require(isValidFeedUrl(value)) { "Feed endpoint must use HTTPS" }
        if (BuildConfig.E2E_FIXTURE) return@runCatching requireNotNull(loadCached())
        FeedParser.parse(download(value.trim()))
    }

    fun restoreDefaultFeedUrl() = preferences.edit().remove("feed_url").apply()

    fun lastCheckedAt(): String? = preferences.getString("last_checked", null)

    fun isValidFeedUrl(value: String): Boolean = runCatching {
        val url = URL(value.trim())
        url.protocol == "https" && url.host.isNotBlank()
    }.getOrDefault(false)

    fun feedUrl(): String = preferences.getString("feed_url", BuildConfig.DEFAULT_FEED_URL).orEmpty()

    fun toggleFavorite(storyId: String): Set<String> {
        val current = favorites().toMutableSet()
        if (!current.add(storyId)) current.remove(storyId)
        preferences.edit().putStringSet("favorites", current).apply()
        return current
    }

    fun favorites(): Set<String> = preferences.getStringSet("favorites", emptySet()).orEmpty()

    private fun download(url: String): String {
        require(url.startsWith("https://")) { "Feed endpoint must use HTTPS" }
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 18_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "JuWen-Android/${BuildConfig.VERSION_NAME}")
        connection.instanceFollowRedirects = true
        return connection.inputStream.bufferedReader().use { it.readText() }.also {
            require(it.length <= 4_000_000) { "Feed response is too large" }
        }
    }

    private fun persistAtomically(raw: String) {
        val temporary = File(context.filesDir, "latest-feed.tmp")
        temporary.writeText(raw)
        try {
            Files.move(
                temporary.toPath(),
                cache.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), cache.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun cdnFallback(url: String): String {
        val marker = "https://github.com/"
        if (!url.startsWith(marker) || !url.contains("/raw/")) return url
        val path = url.removePrefix(marker).replace("/raw/", "@")
        return "https://cdn.jsdelivr.net/gh/$path"
    }
}
