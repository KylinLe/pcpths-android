package com.pcpths.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcpths.app.data.ParsedPage

/** 计划说明页：顶部返回按钮（回进入前的分区）+ 原生内容 */
@Composable
fun DetailPage(page: ParsedPage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(modifier.fillMaxSize()) {
        // 顶栏：返回 + 标题
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = c.ink
                )
            }
            Text(
                page.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 16.dp)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.line)
        )

        // 内容：首屏标题 + 各栏目
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 32.dp)
        ) {
            page.hero?.let { hero ->
                item {
                    Column {
                        if (hero.eyebrow.isNotBlank()) {
                            Text(hero.eyebrow, fontSize = 11.sp, letterSpacing = 2.sp, color = c.muted)
                            Spacer(Modifier.height(14.dp))
                        }
                        Text(
                            hero.title,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = c.ink
                        )
                        if (hero.en.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(hero.en, fontSize = 12.sp, letterSpacing = 3.sp, color = c.muted)
                        }
                        Spacer(Modifier.height(36.dp))
                    }
                }
            }
            page.sections.forEach { section ->
                // 详情页整体已有推入动画，内容不再叠加浮现动画
                SectionTabContent(section, onItemTap = {}, animateItems = false)
            }
        }
    }
}
