package com.pcpths.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.pcpths.app.data.UpdateDownloader
import kotlinx.coroutines.launch

@Composable
fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    PCPTHSTheme {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var detailPath by rememberSaveable { mutableStateOf<String?>(null) }
        var returnTab by rememberSaveable { mutableIntStateOf(0) }
        var bellPulse by rememberSaveable { mutableIntStateOf(0) }
        val snackbarHostState = remember { SnackbarHostState() }
        val context = androidx.compose.ui.platform.LocalContext.current
        val c = LocalAppColors.current

        // 自动更新检测：官网 meta 版本代号 > 本机 versionCode → 弹窗询问（每次同步后校对一次，本会话只问一次）
        var updateDialogVisible by rememberSaveable { mutableStateOf(false) }
        var updatePromptShown by rememberSaveable { mutableStateOf(false) }
        var wasDownloading by remember { mutableStateOf(false) }
        val localVersionCode = remember {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }

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

        // 版本校对：官网代号大于本机 → 询问更新
        LaunchedEffect(state.siteVersionCode) {
            val site = state.siteVersionCode ?: 0
            if (site > localVersionCode && !updatePromptShown) {
                updatePromptShown = true
                updateDialogVisible = true
            }
        }

        // 下载完成 → 交给系统安装器
        // 注意：consumeUpdateReady 会改变本效果的触发条件，必须放在最后调用，
        // 否则效果被自身取消，startActivity 永远执行不到
        LaunchedEffect(state.updateApkReady) {
            if (state.updateApkReady) {
                updateDialogVisible = false
                val file = UpdateDownloader.apkFile(context)
                if (file.exists()) {
                    try {
                        context.startActivity(UpdateDownloader.installIntent(context, file))
                    } catch (e: Exception) {
                        // 设备上没有安装器时静默
                    }
                }
                vm.consumeUpdateReady()
            }
        }

        // 下载结束但未就绪 = 失败 → 关闭弹窗（错误提示由 snackbar 呈现）
        LaunchedEffect(state.updateDownloading) {
            if (state.updateDownloading) {
                wasDownloading = true
            } else if (wasDownloading) {
                wasDownloading = false
                if (!state.updateApkReady) updateDialogVisible = false
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

                            // 详情页：翻页动画——整页从右侧翻入，返回时翻出（纯滑动，无淡入淡出）
                            AnimatedContent(
                                targetState = detailPath != null,
                                transitionSpec = {
                                    if (targetState) {
                                        slideInHorizontally(tween(280, easing = SiteEase)) { it } togetherWith
                                            ExitTransition.None
                                    } else {
                                        EnterTransition.None togetherWith
                                            slideOutHorizontally(tween(280, easing = SiteEase)) { it }
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

        // 更新弹窗：询问 / 下载进度
        if (updateDialogVisible) {
            if (state.updateDownloading) {
                UpdateDialog(
                    title = "正在下载更新",
                    text = "正在下载新版本… ${state.updateProgress ?: 0}%",
                    onDismiss = null,
                    confirmText = null
                )
            } else {
                UpdateDialog(
                    title = "发现新版本",
                    text = "官网已发布新版本，是否立即更新？",
                    onDismiss = { updateDialogVisible = false },
                    confirmText = "更新",
                    onConfirm = { vm.downloadUpdate() }
                )
            }
        }
    }
}

/** 更新弹窗（黑白风格：纸色底、墨色字、强调色按钮） */
@Composable
private fun UpdateDialog(
    title: String,
    text: String,
    onDismiss: (() -> Unit)?,
    confirmText: String?,
    onConfirm: (() -> Unit)? = null
) {
    val c = LocalAppColors.current
    val dismissBtn: (@Composable () -> Unit)? = if (onDismiss != null) {
        {
            TextButton(onClick = onDismiss) {
                Text("暂不", color = c.muted, fontSize = 14.sp, letterSpacing = 1.sp)
            }
        }
    } else null
    val confirmBtn: (@Composable () -> Unit)? = if (confirmText != null && onConfirm != null) {
        {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = c.emphasis,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else null
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        containerColor = c.paper,
        titleContentColor = c.ink,
        textContentColor = c.ink,
        title = {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        },
        text = {
            Text(text, fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.5.sp)
        },
        dismissButton = dismissBtn ?: @Composable {},
        confirmButton = confirmBtn ?: @Composable {}
    )
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
