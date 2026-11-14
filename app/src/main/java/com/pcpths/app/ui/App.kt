package com.pcpths.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pcpths.app.data.Notice
import com.pcpths.app.data.ParsedPage
import com.pcpths.app.data.SiteContent
import com.pcpths.app.data.SiteSync
import kotlinx.coroutines.launch

@Composable
fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    PCPTHSTheme {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var detailPath by rememberSaveable { mutableStateOf<String?>(null) }
        var returnTab by rememberSaveable { mutableIntStateOf(0) }
        var bellPulse by rememberSaveable { mutableIntStateOf(0) }
        val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
        val context = androidx.compose.ui.platform.LocalContext.current
        val c = LocalAppColors.current

        val detailPage by produceState<ParsedPage?>(initialValue = null, detailPath) {
            value = detailPath?.let { vm.parsePage(it) }
        }

        // 打开详情时记住所在分区；返回时回到该分区
        val openDetail: (String) -> Unit = { path ->
            returnTab = selectedTab
            detailPath = path
        }

        // 返回：详情页 → 进入前所在的分区
        BackHandler(enabled = detailPath != null) {
            detailPath = null
            selectedTab = returnTab
        }

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbarHostState.showSnackbar(it)
                vm.consumeMessage()
            }
        }

        Scaffold(
            containerColor = c.paper,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val content = state.content
                when {
                    content == null && state.syncing -> LoadingScreen()
                    content == null -> ErrorScreen { vm.refresh() }
                    else -> {
                        val pageCount = content.tabs.size + 1
                        val pagerState = rememberPagerState(
                            initialPage = selectedTab.coerceIn(0, pageCount - 1)
                        ) { pageCount }
                        val scope = rememberCoroutineScope()

                        // 手势左右滑动 → 底栏选中跟随
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.currentPage }.collect { page ->
                                selectedTab = page.coerceIn(0, pageCount - 1)
                            }
                        }

                        // 统一切标签入口：底栏点击、通知跳转都走这里（详情返回时页面本就停在来源分区，无需滚动）
                        val selectTab: (Int) -> Unit = { index ->
                            detailPath = null
                            selectedTab = index
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }

                        val visibleNotices =
                            content.notices.filter { it.id !in state.dismissedNotices }
                        NoticeStrip(
                            notices = visibleNotices,
                            replayKey = bellPulse,
                            onTap = { n ->
                                detailPath = null
                                handleNoticeTap(
                                    notice = n,
                                    content = content,
                                    context = context,
                                    selectTab = selectTab,
                                    openDetail = openDetail
                                )
                            },
                            onDismiss = { n -> vm.dismissNotice(n.id) }
                        )
                        if (state.offline) OfflineBanner()

                        Box(Modifier.weight(1f)) {
                            // 首页同级页面：左右滑动切换（第 0 页首页，其余按底栏顺序对应栏目）
                            MainTabs(
                                content = content,
                                pagerState = pagerState,
                                userScrollEnabled = detailPath == null,
                                onOpenDetail = openDetail
                            )

                            // 详情页：更深层级，从右侧滑入/滑出覆盖在上方；返回时页面状态原地保留
                            AnimatedContent(
                                targetState = detailPath != null,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInHorizontally(tween(280, easing = SiteEase)) { it / 3 } +
                                                fadeIn(tween(220, easing = SiteEase))) togetherWith
                                                fadeOut(tween(180))
                                    } else {
                                        fadeIn(tween(180)) togetherWith
                                                (slideOutHorizontally(tween(280, easing = SiteEase)) { it / 3 } +
                                                        fadeOut(tween(220, easing = SiteEase)))
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                label = "detail"
                            ) { isDetail ->
                                if (isDetail) {
                                    val page = detailPage
                                    if (page != null) {
                                        DetailPage(page, onBack = {
                                            detailPath = null
                                            selectedTab = returnTab
                                        })
                                    } else {
                                        LoadingScreen()
                                    }
                                }
                            }
                        }

                        BottomCapsuleBar(
                            tabs = content.tabs,
                            selectedIndex = selectedTab,
                            onSelect = selectTab,
                            hasHiddenNotices = content.notices.any { it.id in state.dismissedNotices },
                            onBell = {
                                vm.reshowNotices()
                                bellPulse++
                            },
                            syncing = state.syncing,
                            onRefresh = { vm.refresh() },
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabs(
    content: SiteContent,
    pagerState: PagerState,
    userScrollEnabled: Boolean,
    onOpenDetail: (String) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = userScrollEnabled,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        if (page == 0) {
            HomeTab(content.homeHero)
        } else {
            val section = content.sections.getOrNull(page - 1)
            if (section != null) {
                SectionTab(section, onItemTap = onOpenDetail)
            } else {
                HomeTab(content.homeHero)
            }
        }
    }
}

/**
 * 通知点击的通用规则：
 * - 锚点指向 App 内的栏目 → 切到对应标签
 * - 锚点指向 App 不展示的栏目（如下载板块）→ 用默认浏览器打开官网对应位置
 * - 站内页面 → App 内打开
 * - 外部链接 → 用默认浏览器打开
 */
private fun handleNoticeTap(
    notice: Notice,
    content: SiteContent,
    context: Context,
    selectTab: (Int) -> Unit,
    openDetail: (String) -> Unit
) {
    val target = notice.target
    when {
        target.startsWith("#") -> {
            val id = target.removePrefix("#")
            val index = content.tabs.indexOfFirst { it.id == id }
            if (index >= 0) {
                selectTab(index + 1)
            } else {
                openInBrowser(context, SiteSync.BASE_URL + "index.html" + target)
            }
        }
        target.startsWith("http") -> openInBrowser(context, target)
        target.isNotBlank() -> {
            val path = target.substringBefore("#").substringBefore("?")
                .removePrefix(SiteSync.BASE_URL).removePrefix("/pcpths/")
            openDetail(path)
        }
    }
}

private fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // 设备上没有浏览器时静默失败，不做处理
    }
}

@Composable
private fun LoadingScreen() {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.paper),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = c.ink, strokeWidth = 2.dp)
        Spacer(Modifier.height(16.dp))
        Text("正在同步官网内容…", fontSize = 13.sp, letterSpacing = 1.sp, color = c.muted)
    }
}

@Composable
private fun ErrorScreen(onRetry: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("无法连接官网", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("请检查网络后重试", fontSize = 13.sp, color = c.muted)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .border(1.dp, c.emphasis, RoundedCornerShape(50))
                .clickable(onClick = onRetry)
                .padding(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text("重试", fontSize = 14.sp, letterSpacing = 2.sp, color = c.ink)
        }
    }
}

@Composable
private fun OfflineBanner() {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.line)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("当前离线 · 显示上次同步的内容", fontSize = 11.sp, letterSpacing = 1.sp, color = c.muted)
    }
}
