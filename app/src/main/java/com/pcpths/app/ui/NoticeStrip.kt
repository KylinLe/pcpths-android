package com.pcpths.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcpths.app.data.Notice
import kotlinx.coroutines.delay

/**
 * 黑底通知条（官网 .notices 的原生版）
 * 动画：进入时从顶部滑入展开（官网同款缓动）；关闭时滑出收起；replayKey 变化时整条重播滑入（官网 repop 行为）
 */
@Composable
fun NoticeStrip(
    notices: List<Notice>,
    replayKey: Int,
    onTap: (Notice) -> Unit,
    onDismiss: (Notice) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    // 展示顺序 + 每条可见状态（移除时先播退出动画再真正移除）
    val order = remember { mutableStateListOf<Notice>() }
    val shown = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(notices, replayKey) {
        // 同步数据与顺序
        notices.forEach { n ->
            val i = order.indexOfFirst { it.id == n.id }
            if (i >= 0) order[i] = n else order.add(n)
        }
        // 已关闭的通知：播退出动画缩到零高度，但不从 order 移除（界面结构不变，其他通知不受影响）
        val activeIds = notices.map { it.id }.toSet()
        order.forEach { n -> if (n.id !in activeIds) shown[n.id] = false }
        // 铃铛重播：全部先隐藏再重新滑入
        if (replayKey > 0) {
            order.forEach { shown[it.id] = false }
            delay(50)
        }
        order.forEachIndexed { i, n ->
            if (n.id in activeIds && shown[n.id] != true) {
                delay(if (i == 0) 160 else 50)
                shown[n.id] = true
            }
        }
    }

    if (order.isEmpty()) return

    // 关闭的通知不真正移除：收缩到零高度留在原位，界面结构不变，
    // 其他通知的动画状态永远不会被重置（避免"收起又弹出"）
    val visibleIds = order.filter { shown[it.id] == true }.map { it.id }

    Column(modifier.fillMaxWidth().background(c.noticeBg)) {
        order.forEach { n ->
            // 稳定 key：每条通知的动画状态按 id 隔离
            androidx.compose.runtime.key(n.id) {
                AnimatedVisibility(
                    visible = shown[n.id] == true,
                    enter = slideInVertically(tween(500, easing = SiteEase)) { -it } +
                            expandVertically(tween(500, easing = SiteEase)) +
                            fadeIn(tween(500, easing = SiteEase)),
                    exit = slideOutVertically(tween(300, easing = SiteEase)) { -it } +
                            shrinkVertically(tween(300, easing = SiteEase)) +
                            fadeOut(tween(300, easing = SiteEase))
                ) {
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onTap(n) }
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    n.title,
                                    color = c.noticeFg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                                if (n.more.isNotBlank()) {
                                    Text(
                                        n.more,
                                        color = c.noticeMore,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            IconButton(onClick = { onDismiss(n) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "关闭通知",
                                    tint = c.noticeFg,
                                    modifier = Modifier.height(16.dp)
                                )
                            }
                        }
                        if (visibleIds.isNotEmpty() && n.id != visibleIds.last()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(c.noticeDivider)
                            )
                        }
                    }
                }
            }
        }
    }
}
