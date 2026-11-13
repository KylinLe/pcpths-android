package com.pcpths.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcpths.app.R
import com.pcpths.app.data.HeroBlock

/**
 * 首页分区：居中式设计——顶部社团 logo，首屏「留住光」文字居中 + 六边形光圈（按官网 SVG 原生重绘）
 * 动画（性能版）：
 * - 光圈随滚动旋转：只在图形层更新，不触发页面重算（官网同款 min(scrollY*0.06, 36)）
 * - 深色模式呼吸光晕：只重绘画布，且仅深色模式才运行
 */
@Composable
fun HomeTab(hero: HeroBlock, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 社团 logo（相机图形，随主题着色：浅色黑、深色白）
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "普中摄影社",
            colorFilter = ColorFilter.tint(c.ink),
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(40.dp))
        if (hero.eyebrow.isNotBlank()) {
            Text(
                hero.eyebrow,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = c.muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
        }
        Text(
            hero.title,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            color = c.ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            hero.en,
            fontSize = 14.sp,
            letterSpacing = 3.sp,
            color = c.muted,
            textAlign = TextAlign.Center
        )
        hero.desc?.let { desc ->
            Spacer(Modifier.height(24.dp))
            Text(
                desc,
                fontSize = 16.sp,
                lineHeight = 28.sp,
                color = c.ink,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(44.dp))
        ApertureGraphic(
            Modifier
                .fillMaxWidth(0.88f)
                // 绘制阶段读取滚动值：只更新图形层，不触发整页重算
                .graphicsLayer { rotationZ = minOf(scrollState.value * 0.06f, 36f) }
        )
    }
}

/** 六边形光孔（官网首屏 SVG 的原生重绘） */
@Composable
fun ApertureGraphic(modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    // 光晕呼吸动画：仅深色模式创建；数值只在画布绘制阶段读取，不触发组件重算
    val glowTransition =
        if (c.glow.alpha > 0f) rememberInfiniteTransition(label = "glow") else null
    val glowAnim = glowTransition?.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Canvas(modifier) {
        val s = size.minDimension / 400f
        val stroke = { w: Float -> Stroke(w * s) }
        val ink = c.emphasis
        val glowAlpha = if (c.glow.alpha > 0f) (glowAnim?.value ?: 0f) else 0f

        // 光晕（仅深色模式）
        if (glowAlpha > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c.glow.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.66f
                )
            )
        }

        translate(left = 0f, top = 14f * s) {
            // 两个外圈
            drawCircle(ink, radius = 170f * s, center = Offset(200f * s, 200f * s), style = stroke(2.4f))
            drawCircle(ink, radius = 162f * s, center = Offset(200f * s, 200f * s), style = stroke(1f))

            // 六条刻度线
            val lines = listOf(
                361f to 200f, 344f to 200f,
                280.5f to 339.43f, 272f to 324.71f,
                119.5f to 339.43f, 128f to 324.71f,
                39f to 200f, 56f to 200f,
                119.5f to 60.57f, 128f to 75.29f,
                280.5f to 60.57f, 272f to 75.29f
            )
            lines.chunked(2).forEach { (a, b) ->
                drawLine(
                    ink,
                    Offset(a.first * s, a.second * s),
                    Offset(b.first * s, b.second * s),
                    strokeWidth = 1.6f * s
                )
            }

            // 六边形
            val hex = listOf(
                300f to 200f, 250f to 286.6f, 150f to 286.6f,
                100f to 200f, 150f to 113.4f, 250f to 113.4f
            )
            val path = Path().apply {
                moveTo(hex[0].first * s, hex[0].second * s)
                hex.drop(1).forEach { lineTo(it.first * s, it.second * s) }
                close()
            }
            drawPath(path, ink, style = stroke(2f))

            // 六个角上的小圆点（纸色填充）
            hex.forEach {
                drawCircle(c.paper, radius = 4f * s, center = Offset(it.first * s, it.second * s))
                drawCircle(ink, radius = 4f * s, center = Offset(it.first * s, it.second * s), style = stroke(1.4f))
            }

            // 顶部竖线
            drawLine(
                ink,
                Offset(200f * s, 2f * s),
                Offset(200f * s, 22f * s),
                strokeWidth = 2.4f * s
            )
        }
    }
}
