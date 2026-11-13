package com.pcpths.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 官网设计语言：细线 / 黑白 / 字距 / 六边形光孔
 * 深色模式 = 官网黑白系统的"反相"（黑底白字、白底通知条、白色强调线）
 */
data class AppColors(
    val ink: Color,        // 主文字
    val paper: Color,      // 页面背景
    val muted: Color,      // 次要文字
    val faint: Color,      // 最弱文字
    val line: Color,       // 浅灰结构线
    val emphasis: Color,   // 强调位：列表顶部起始线、按钮描边、胶囊选中态
    val noticeBg: Color,   // 通知条背景
    val noticeFg: Color,   // 通知条文字
    val noticeMore: Color, // 通知条次要文字
    val noticeDivider: Color,
    val barBg: Color,      // 底部导航栏背景
    val glow: Color        // 深色模式下光孔光晕（亮色模式为透明）
)

private val LightColors = AppColors(
    ink = Color(0xFF0F0F0F),
    paper = Color(0xFFFFFFFF),
    muted = Color(0xFF6E6E73),
    faint = Color(0xFFA1A1A6),
    line = Color(0xFFE3E3E5),
    emphasis = Color(0xFF0F0F0F),
    noticeBg = Color(0xFF0F0F0F),
    noticeFg = Color(0xFFFFFFFF),
    noticeMore = Color(0xB3FFFFFF),
    noticeDivider = Color(0x2EFFFFFF),
    barBg = Color(0xFFFFFFFF),
    glow = Color.Transparent
)

private val DarkColors = AppColors(
    ink = Color(0xFFFFFFFF),
    paper = Color(0xFF0F0F0F),
    muted = Color(0xFFA1A1A6),
    faint = Color(0xFF6E6E73),
    line = Color(0xFF2C2C2E),
    emphasis = Color(0xFFFFFFFF),
    noticeBg = Color(0xFFFFFFFF),
    noticeFg = Color(0xFF0F0F0F),
    noticeMore = Color(0xB30F0F0F),
    noticeDivider = Color(0x2E0F0F0F),
    barBg = Color(0xFF161618),
    glow = Color(0xFFFFFFFF)
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

/** 官网同款缓动曲线：cubic-bezier(.22,.61,.24,1) */
val SiteEase = CubicBezierEasing(0.22f, 0.61f, 0.24f, 1f)

@Composable
fun PCPTHSTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = c.emphasis,
                    onPrimary = c.paper,
                    background = c.paper,
                    onBackground = c.ink,
                    surface = c.paper,
                    onSurface = c.ink,
                    surfaceVariant = c.paper,
                    onSurfaceVariant = c.muted,
                    outline = c.line,
                    error = Color(0xFFFFB4AB),
                    onError = Color(0xFF690005)
                )
            } else {
                lightColorScheme(
                    primary = c.emphasis,
                    onPrimary = c.paper,
                    background = c.paper,
                    onBackground = c.ink,
                    surface = c.paper,
                    onSurface = c.ink,
                    surfaceVariant = c.paper,
                    onSurfaceVariant = c.muted,
                    outline = c.line,
                    error = Color(0xFFB3261E),
                    onError = Color.White
                )
            },
            content = content
        )
    }
}
