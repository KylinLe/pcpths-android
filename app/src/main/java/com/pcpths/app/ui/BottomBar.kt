package com.pcpths.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcpths.app.data.TabInfo

/**
 * 底部胶囊导航栏：
 * - 标签自动来自官网栏目，放不下可左右滑动
 * - 右端固定铃铛（重新显示已关闭的通知）和刷新（同步时图标旋转）
 * - 深色模式：选中态反相为白色胶囊
 */
@Composable
fun BottomCapsuleBar(
    tabs: List<TabInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    hasHiddenNotices: Boolean,
    onBell: () -> Unit,
    syncing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    Surface(
        modifier,
        shape = RoundedCornerShape(percent = 50),
        color = c.barBg,
        border = BorderStroke(1.dp, c.line),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(selectedIndex) {
                listState.animateScrollToItem(selectedIndex)
            }
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    CapsuleTab("首页", selectedIndex == 0) { onSelect(0) }
                }
                items(tabs.size) { i ->
                    CapsuleTab(tabs[i].label, selectedIndex == i + 1) {
                        onSelect(i + 1)
                    }
                }
            }

            // 细线分隔
            Box(
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(c.line)
            )

            // 铃铛：有已关闭的通知时显示小圆点
            IconButton(onClick = onBell) {
                Box {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "重新显示通知",
                        tint = c.ink,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasHiddenNotices) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(c.emphasis)
                        )
                    }
                }
            }

            // 刷新：仅在同步中才创建旋转动画（平时零开销）
            IconButton(onClick = onRefresh, enabled = !syncing) {
                if (syncing) {
                    val rotation by rememberInfiniteTransition(label = "refresh").animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "refreshRotation"
                    )
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        tint = c.ink,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                } else {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        tint = c.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.emphasis else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) c.paper else c.ink,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
