package cc.juwen.reader

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import cc.juwen.reader.model.FeedSnapshot
import cc.juwen.reader.model.NewsSource
import cc.juwen.reader.model.Story
import org.junit.Rule
import org.junit.Test

class NewsScreenPaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_10.copy(screenWidth = 2560, screenHeight = 1600),
    )

    @Test
    fun landscapeLight() {
        paparazzi.snapshot(name = "redmi-pad-pro-landscape-light") {
            JuwenTheme(forceDark = false) { TestNewsScreen(sampleSnapshot()) }
        }
    }

    @Test
    fun landscapeDark() {
        paparazzi.snapshot(name = "redmi-pad-pro-landscape-dark") {
            JuwenTheme(forceDark = true) { TestNewsScreen(sampleSnapshot()) }
        }
    }

    @Test
    fun landscapeSelectedDetail() {
        val snapshot = sampleSnapshot()
        paparazzi.snapshot(name = "redmi-pad-pro-selected-detail") {
            JuwenTheme(forceDark = false) {
                TestNewsScreen(snapshot, selectedStory = snapshot.stories.first())
            }
        }
    }

    @Test
    fun emptyOffline() {
        paparazzi.snapshot(name = "redmi-pad-pro-empty-offline") {
            JuwenTheme(forceDark = false) { TestNewsScreen(FeedSnapshot(1, "2026-08-15T08:00:00Z", "rules", emptyList()), error = "无法连接数据源，正在显示本地内容") }
        }
    }
}

class NewsScreenPortraitPaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_10.copy(screenWidth = 1600, screenHeight = 2560),
    )

    @Test
    fun portraitLongTitle() {
        val longStory = sampleSnapshot().stories.first().copy(
            title = "这是一条用于验证红米平板大字号和超长中文标题不会遮挡后续新闻内容的测试标题",
        )
        paparazzi.snapshot(name = "redmi-pad-pro-portrait-long-title") {
            JuwenTheme(forceDark = false) { TestNewsScreen(sampleSnapshot().copy(stories = listOf(longStory) + sampleSnapshot().stories.drop(1)), twoPane = false) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TestNewsScreen(snapshot: FeedSnapshot, error: String? = null, twoPane: Boolean = true, selectedStory: Story? = null) {
    NewsScreen(
        destination = Destination.Today,
        snapshot = snapshot,
        selectedStory = selectedStory,
        favorites = emptySet(),
        refreshing = false,
        error = error,
        onRefresh = {},
        onStory = {},
        onStoryClose = {},
        onFavorite = {},
        twoPane = twoPane,
    )
}

private fun sampleSnapshot(): FeedSnapshot = FeedSnapshot(
    schemaVersion = 1,
    generatedAt = "2026-08-15T08:00:00Z",
    mode = "rules",
    stories = listOf(
        Story("visual-ai", "国产AI芯片发布新进展", "科技", "2026-08-15T07:45:00Z", "2026-08-15T08:00:00Z", 48.0, true, "来自两个来源的规则化热点样本。", listOf(NewsSource("百度热搜", "https://www.baidu.com/s?wd=ai-chip", 1), NewsSource("微博", "https://weibo.com/topic/ai-chip", 2))),
        Story("visual-policy", "多地公布新一轮消费政策", "时政", "2026-08-15T07:20:00Z", "2026-08-15T08:00:00Z", 26.5, false, null, listOf(NewsSource("百度热搜", "https://www.baidu.com/s?wd=policy", 4))),
        Story("visual-cars", "新能源汽车月度数据更新", "汽车", "2026-08-15T06:58:00Z", "2026-08-15T08:00:00Z", 25.1, false, null, listOf(NewsSource("微博", "https://weibo.com/topic/cars", 5))),
    ),
)
