package cc.juwen.reader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cc.juwen.reader.data.FeedRepository
import cc.juwen.reader.model.FeedSnapshot
import cc.juwen.reader.model.Story
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { JuwenTheme { JuwenApp() } }
    }
}

internal enum class Destination(val label: String, val icon: ImageVector) {
    Today("今日", Icons.Outlined.Today),
    Categories("分类", Icons.Outlined.GridView),
    Briefs("简报", Icons.Outlined.AutoStories),
    Favorites("收藏", Icons.Outlined.BookmarkBorder),
    Settings("设置", Icons.Outlined.Settings),
}

private val Accent = Color(0xFF007AFF)
private val LightBackground = Color(0xFFF7F7F5)
private val DarkBackground = Color(0xFF111112)

@Composable
internal fun JuwenTheme(forceDark: Boolean? = null, content: @Composable () -> Unit) {
    val dark = forceDark ?: androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(primary = Color(0xFF0A84FF), background = DarkBackground, surface = Color(0xFF1C1C1E))
        else lightColorScheme(primary = Accent, background = LightBackground, surface = Color.White),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, letterSpacing = 0.sp),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, letterSpacing = 0.sp),
            titleLarge = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.sp),
            titleMedium = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.sp),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.sp),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
        ),
        content = content,
    )
}

@Composable
private fun JuwenApp() {
    val context = LocalContext.current
    val repository = remember { FeedRepository(context) }
    var destination by remember { mutableStateOf(Destination.Today) }
    var snapshot by remember { mutableStateOf(repository.loadCached()) }
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    var favorites by remember { mutableStateOf(repository.favorites()) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkedAt by remember { mutableStateOf(repository.lastCheckedAt()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            repository.refresh().fold(
                onSuccess = { snapshot = it; error = null },
                onFailure = { error = "无法连接数据源，正在显示本地内容" },
            )
            checkedAt = repository.lastCheckedAt()
            refreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                AppRail(destination, onSelect = { destination = it; selectedStory = null })
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AppContent(
                    modifier = Modifier.weight(1f), destination, snapshot, selectedStory, favorites,
                    refreshing, error, checkedAt, onRefresh = ::refresh, onStory = { selectedStory = it },
                    onStoryClose = { selectedStory = null },
                    onFavorite = { favorites = repository.toggleFavorite(it.id) },
                    onFeedUrl = repository::setFeedUrl, onTestFeedUrl = repository::testFeedUrl, feedUrl = repository.feedUrl(), twoPane = true,
                )
            }
        } else {
            Scaffold(bottomBar = { AppBottomBar(destination) { destination = it; selectedStory = null } }) { padding ->
                AppContent(
                    modifier = Modifier.padding(padding), destination, snapshot, selectedStory, favorites,
                    refreshing, error, checkedAt, onRefresh = ::refresh, onStory = { selectedStory = it },
                    onStoryClose = { selectedStory = null },
                    onFavorite = { favorites = repository.toggleFavorite(it.id) },
                    onFeedUrl = repository::setFeedUrl, onTestFeedUrl = repository::testFeedUrl, feedUrl = repository.feedUrl(), twoPane = false,
                )
            }
        }
    }
}

@Composable
private fun AppRail(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f), modifier = Modifier.width(88.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("聚闻", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(24.dp))
        Destination.entries.forEach { item ->
            NavigationRailItem(
                selected = selected == item,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun AppBottomBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar {
        Destination.entries.forEach { item ->
            NavigationBarItem(selected == item, { onSelect(item) }, { Icon(item.icon, null) }, label = { Text(item.label) })
        }
    }
}

@Composable
private fun AppContent(
    modifier: Modifier,
    destination: Destination,
    snapshot: FeedSnapshot?,
    selectedStory: Story?,
    favorites: Set<String>,
    refreshing: Boolean,
    error: String?,
    checkedAt: String?,
    onRefresh: () -> Unit,
    onStory: (Story) -> Unit,
    onStoryClose: () -> Unit,
    onFavorite: (Story) -> Unit,
    onFeedUrl: (String) -> Unit,
    onTestFeedUrl: (String) -> Result<FeedSnapshot>,
    feedUrl: String,
    twoPane: Boolean,
) {
    AnimatedContent(destination, modifier.fillMaxSize(), label = "destination") { current ->
        when (current) {
            Destination.Settings -> SettingsScreen(feedUrl, onFeedUrl, onTestFeedUrl)
            else -> NewsScreen(current, snapshot, selectedStory, favorites, refreshing, error, checkedAt, onRefresh, onStory, onStoryClose, onFavorite, twoPane)
        }
    }
}

@Composable
internal fun NewsScreen(
    destination: Destination,
    snapshot: FeedSnapshot?,
    selectedStory: Story?,
    favorites: Set<String>,
    refreshing: Boolean,
    error: String?,
    checkedAt: String? = null,
    onRefresh: () -> Unit,
    onStory: (Story) -> Unit,
    onStoryClose: () -> Unit,
    onFavorite: (Story) -> Unit,
    twoPane: Boolean,
) {
    var category by remember { mutableStateOf("全部") }
    var query by remember { mutableStateOf("") }
    val all = snapshot?.stories.orEmpty()
    val categories = listOf("全部") + all.map { it.category }.distinct()
    val visible = all.filter {
        (destination != Destination.Favorites || it.id in favorites) &&
            (category == "全部" || it.category == category) &&
            (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.sources.any { source -> source.name.contains(query, true) })
    }

    if (!twoPane && selectedStory != null) {
        StoryDetail(selectedStory, selectedStory.id in favorites, onFavorite, onBack = onStoryClose)
        return
    }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.weight(if (twoPane) .43f else 1f).fillMaxHeight().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(destination.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        snapshot?.let { "${it.stories.size} 条 · 数据生成 ${friendlyTime(it.generatedAt)}" } ?: "尚无可用数据",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onRefresh, enabled = !refreshing) { Icon(Icons.Outlined.Refresh, "刷新") }
            }
            FeedStatus(snapshot, refreshing, error, checkedAt)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text("搜索新闻或来源") })
            if (destination == Destination.Categories || destination == Destination.Today) {
                ScrollableTabRow(selectedTabIndex = categories.indexOf(category).coerceAtLeast(0), edgePadding = 0.dp, divider = {}) {
                    categories.forEach { item -> Tab(category == item, { category = item }, text = { Text(item) }) }
                }
            }
            if (visible.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (refreshing) Icons.Outlined.Sync else Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(if (refreshing) "正在获取最新新闻" else "没有可显示的新闻", fontWeight = FontWeight.SemiBold)
                    Text(if (error != null) "请检查网络或前往设置测试数据源" else "尝试调整分类或搜索条件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(visible, key = { _, item -> item.id }) { index, story -> StoryRow(index + 1, story, story.id in favorites, selectedStory?.id == story.id, onStory, onFavorite) }
            }
        }
        if (twoPane) {
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(.57f).fillMaxHeight()) {
                if (selectedStory != null) StoryDetail(selectedStory, selectedStory.id in favorites, onFavorite)
                else BriefPlaceholder(snapshot)
            }
        }
    }
}

@Composable
private fun FeedStatus(snapshot: FeedSnapshot?, refreshing: Boolean, error: String?, checkedAt: String?) {
    val partial = snapshot?.failedSourceCount?.let { it > 0 } == true
    val icon = when {
        refreshing -> Icons.Outlined.Sync
        error != null -> Icons.Outlined.CloudOff
        partial -> Icons.Outlined.WarningAmber
        else -> Icons.Outlined.CheckCircle
    }
    val text = when {
        refreshing -> "正在检查更新"
        error != null -> "离线缓存 · $error"
        partial -> "部分来源异常 · ${snapshot?.successfulSourceCount} 个来源可用"
        checkedAt != null -> "已是最新 · 检查于 ${friendlyTime(checkedAt)}"
        else -> "本地基线内容"
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), shape = RoundedCornerShape(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(7.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StoryRow(rank: Int, story: Story, favorite: Boolean, selected: Boolean, onStory: (Story) -> Unit, onFavorite: (Story) -> Unit) {
    Surface(color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else Color.Transparent, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().clickable { onStory(story) }) {
        Row(Modifier.padding(vertical = 9.dp, horizontal = 8.dp), verticalAlignment = Alignment.Top) {
            Text(rank.toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp).padding(top = 2.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (story.important) Text("重要", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
                    Text(story.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text("${story.category} · ${story.sources.size} 个来源 · ${story.sources.firstOrNull()?.name.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onFavorite(story) }) { Icon(if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (favorite) "取消收藏" else "收藏") }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun BriefPlaceholder(snapshot: FeedSnapshot?) {
    Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.Center) {
        Text("今日焦点", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text("已整理 ${snapshot?.stories?.size ?: 0} 条新闻。选择左侧标题，在这里安静阅读来源与详情。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StoryDetail(story: Story, favorite: Boolean, onFavorite: (Story) -> Unit, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(36.dp)) {
        item {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
                Spacer(Modifier.height(8.dp))
            }
            Text(story.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text(story.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("综合分 ${"%.1f".format(story.score)} · ${story.sources.size} 个来源", color = MaterialTheme.colorScheme.onSurfaceVariant)
            story.summary?.let { Spacer(Modifier.height(20.dp)); Text(it, style = MaterialTheme.typography.bodyLarge) }
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { story.sources.firstOrNull()?.url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("查看原文") }
                OutlinedButton(onClick = { onFavorite(story) }) { Icon(if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, null); Spacer(Modifier.width(8.dp)); Text(if (favorite) "已收藏" else "收藏") }
            }
            Spacer(Modifier.height(28.dp))
            Text("相关来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(story.sources) { source ->
            ListItem(headlineContent = { Text(source.name) }, supportingContent = { Text("榜单排名 ${source.rank}") }, trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }, modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun SettingsScreen(initialUrl: String, onFeedUrl: (String) -> Unit, onTestFeedUrl: (String) -> Result<FeedSnapshot>) {
    var url by remember { mutableStateOf(initialUrl) }
    var status by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(28.dp))
        Text("数据源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(url, { url = it; status = null }, Modifier.fillMaxWidth(), label = { Text("GitHub Pages feed URL") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !testing,
            onClick = {
                testing = true
                status = "正在测试连接"
                scope.launch {
                    val result = withContext(Dispatchers.IO) { onTestFeedUrl(url) }
                    result.fold(
                        onSuccess = {
                            onFeedUrl(url)
                            status = "连接成功，已保存 ${it.stories.size} 条新闻的数据源"
                        },
                        onFailure = { status = "连接失败，已保留原数据源" },
                    )
                    testing = false
                }
            },
        ) { Icon(Icons.Outlined.WifiTethering, null); Spacer(Modifier.width(8.dp)); Text(if (testing) "正在测试" else "测试并保存") }
        status?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(28.dp))
        Text("新闻源每 30 分钟生成一次。你可以随时在首页手动刷新；刷新失败时会继续显示上一次有效内容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
        Text("关于聚闻", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("版本 1.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("开发者 · LKD工作室", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun friendlyTime(value: String): String = value.replace("T", " ").replace("Z", " UTC")
