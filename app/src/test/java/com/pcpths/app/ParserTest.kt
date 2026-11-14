package com.pcpths.app

import com.pcpths.app.data.Block
import com.pcpths.app.data.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 用官网当前页面的快照验证解析器 */
class ParserTest {

    private fun fixture(name: String): String =
        File("src/test/resources/fixtures/$name").readText()

    @Test
    fun `首页解析出三个栏目标签`() {
        val c = Parser.parseHome(fixture("index.html"))
        assertEquals(listOf("courses", "missions", "plans"), c.tabs.map { it.id })
        assertEquals(listOf("课程", "任务", "计划"), c.tabs.map { it.label })
    }

    @Test
    fun `官网版本代号读取正确`() {
        assertEquals(3, Parser.appVersionCode(fixture("index.html")))
        // 缺失 / 非法时返回 null，不触发更新
        assertNull(Parser.appVersionCode("<html><head></head><body></body></html>"))
        assertNull(Parser.appVersionCode("<meta name=\"app-version-code\" content=\"abc\">"))
        assertNull(Parser.appVersionCode("<meta name=\"app-version-code\">"))
    }

    @Test
    fun `下载板块被排除`() {
        val c = Parser.parseHome(fixture("index.html"))
        assertNull(c.tabs.find { it.id == "app" })
    }

    @Test
    fun `首屏内容正确`() {
        val c = Parser.parseHome(fixture("index.html"))
        assertEquals("留住光", c.homeHero.title)
        assertEquals("Catch the Light", c.homeHero.en)
        assertTrue(c.homeHero.desc!!.contains("快门"))
    }

    @Test
    fun `通知解析正确`() {
        val c = Parser.parseHome(fixture("index.html"))
        assertEquals(2, c.notices.size)
        val plans = c.notices.find { it.id == "notice-plans" }!!
        assertEquals("拍摄计划更新", plans.title)
        assertEquals("#plans", plans.target)
    }

    @Test
    fun `计划列表条目带链接`() {
        val c = Parser.parseHome(fixture("index.html"))
        val plansSection = c.sections.find { it.id == "plans" }!!
        val list = plansSection.blocks.filterIsInstance<Block.ListBlock>().first()
        assertEquals(2, list.items.size)
        assertTrue(list.items.all { it.link != null })
        assertEquals("元旦晚会拍摄与录制", list.items[1].t)
    }

    @Test
    fun `课程卡片解析正确`() {
        val c = Parser.parseHome(fixture("index.html"))
        val coursesSection = c.sections.find { it.id == "courses" }!!
        val courses = coursesSection.blocks.filterIsInstance<Block.CoursesBlock>().first()
        assertEquals(2, courses.items.size)
        assertEquals("基础课", courses.items[0].title)
        assertEquals("Foundation", courses.items[0].en)
    }

    @Test
    fun `计划说明页解析岗位表`() {
        val p = Parser.parsePage(fixture("plan/gala-recording.html"))
        assertEquals("元旦晚会拍摄与录制", p.title)
        val table = p.sections[0].blocks.filterIsInstance<Block.TableBlock>().first()
        assertEquals(listOf("岗位", "设备要求", "需拍摄内容", "备注"), table.headers)
        assertEquals(4, table.rows.size)
        assertEquals("台前拍摄", table.rows[0].title)
        assertEquals("3 人", table.rows[0].count)
        assertEquals("设备要求", table.rows[0].cells[0].first)
    }

    @Test
    fun `计划说明页解析要求列表`() {
        val p = Parser.parsePage(fixture("plan/clubs-greeting.html"))
        assertEquals("元旦各社团祝福拍摄", p.title)
        val list = p.sections[0].blocks.filterIsInstance<Block.ListBlock>().first()
        assertEquals(3, list.items.size)
        assertEquals("4K · 30fps · 16:9 · 声音清晰", list.items[2].d)
    }
}
