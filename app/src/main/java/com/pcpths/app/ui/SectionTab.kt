package com.pcpths.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pcpths.app.data.Block
import com.pcpths.app.data.SectionBlock

/** 栏目分区：按官网细线规范原生渲染一个栏目的全部内容块 */
@Composable
fun SectionTab(
    section: SectionBlock,
    onItemTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 36.dp)
    ) {
        SectionTabContent(section, onItemTap)
    }
}

/** 把栏目内容追加进任意列表（用于详情页复用）。无浮现动画：条目即时显示 */
fun LazyListScope.SectionTabContent(
    section: SectionBlock,
    onItemTap: (String) -> Unit
) {
    item { SectionHead(section.num, section.title, section.en) }
    section.blocks.forEach { block ->
        when (block) {
            is Block.Para -> item { BlockPara(block) }
            is Block.ListBlock -> item { BlockList(block, onItemTap) }
            is Block.CoursesBlock -> item { BlockCourses(block) }
            is Block.TableBlock -> item { BlockTable(block) }
        }
    }
}

@Composable
private fun SectionHead(num: String, title: String, en: String) {
    val c = LocalAppColors.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (num.isNotBlank()) {
                Text(num, fontSize = 14.sp, letterSpacing = 2.sp, color = c.muted)
                Spacer(Modifier.width(14.dp))
            }
            Text(
                title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = c.ink
            )
        }
        if (en.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(en, fontSize = 11.sp, letterSpacing = 4.sp, color = c.faint)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun BlockPara(block: Block.Para) {
    val c = LocalAppColors.current
    Column {
        Text(block.text, fontSize = 15.sp, lineHeight = 26.sp, color = c.ink)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BlockList(block: Block.ListBlock, onItemTap: (String) -> Unit) {
    val c = LocalAppColors.current
    Column {
        ThinTopLine()
        block.items.forEachIndexed { index, item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = item.link != null) { item.link?.let(onItemTap) }
                    .padding(vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.n.isNotBlank()) {
                        Text(item.n, fontSize = 12.sp, letterSpacing = 1.sp, color = c.muted)
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        item.t,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.link != null) {
                        Box(
                            Modifier
                                .border(1.dp, c.emphasis, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "进入 →",
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = c.ink
                            )
                        }
                    }
                }
                item.d?.let { d ->
                    Spacer(Modifier.height(4.dp))
                    Text(d, fontSize = 13.sp, lineHeight = 20.sp, color = c.muted)
                }
            }
            if (index != block.items.lastIndex) ThinDivider()
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun BlockCourses(block: Block.CoursesBlock) {
    val c = LocalAppColors.current
    Column {
        ThinTopLine()
        block.items.forEachIndexed { index, course ->
            Column(Modifier.padding(vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(course.idx, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            course.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = c.ink
                        )
                        if (course.en.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(course.en, fontSize = 11.sp, letterSpacing = 3.sp, color = c.faint)
                        }
                        if (course.desc.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(course.desc, fontSize = 13.sp, lineHeight = 22.sp, color = c.muted)
                        }
                    }
                }
            }
            if (index != block.items.lastIndex) ThinDivider()
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun BlockTable(block: Block.TableBlock) {
    val c = LocalAppColors.current
    Column {
        ThinTopLine()
        block.rows.forEachIndexed { index, row ->
            Column(Modifier.padding(vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    row.count?.let { count ->
                        Box(
                            Modifier
                                .border(1.dp, c.emphasis, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(count, fontSize = 11.sp, letterSpacing = 1.sp, color = c.ink)
                        }
                    }
                }
                row.cells.forEach { (label, value) ->
                    if (value.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(label, fontSize = 11.sp, letterSpacing = 2.sp, color = c.faint)
                        Spacer(Modifier.height(3.dp))
                        Text(value, fontSize = 14.sp, lineHeight = 22.sp, color = c.ink)
                    }
                }
            }
            if (index != block.rows.lastIndex) ThinDivider()
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ThinTopLine() {
    val c = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.emphasis)
    )
}

@Composable
private fun ThinDivider() {
    val c = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.line)
    )
}
