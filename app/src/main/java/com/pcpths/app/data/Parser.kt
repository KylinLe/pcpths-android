package com.pcpths.app.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 官网网页 → 原生界面数据 的翻译器。
 * 只认官网既有组件（sec / list / courses / spec-table / notice / hero），
 * 未知内容安全跳过，不崩溃。
 */
object Parser {

    fun parseHome(html: String): SiteContent {
        val doc = Jsoup.parse(html)

        // 顶部导航的短标签（如 课程→#courses），作为底栏标签名
        val navLabels = doc.select("header .nav-links a").mapNotNull { a ->
            val href = a.attr("href")
            if (href.startsWith("#")) href.substring(1) to a.text().trim() else null
        }.toMap()

        val notices = doc.select(".notice").map { n ->
            val link = n.selectFirst(".notice-link")
            Notice(
                id = n.id().ifBlank { n.selectFirst(".t")?.text().orEmpty() },
                title = n.selectFirst(".t")?.text().orEmpty(),
                more = n.selectFirst(".more")?.text().orEmpty(),
                target = link?.attr("href").orEmpty()
            )
        }

        val sections = doc.select("main > section.sec").mapNotNull { sec ->
            // App 不展示下载板块（含下载按钮的栏目）
            if (sec.selectFirst(".dl-opts, .dl-btn") != null) return@mapNotNull null
            parseSection(sec)
        }

        val heroEl = doc.selectFirst("main > section.hero")
        val hero = heroEl?.let { h ->
            HeroBlock(
                eyebrow = h.selectFirst(".eyebrow")?.text().orEmpty(),
                title = h.selectFirst("h1")?.text().orEmpty(),
                en = h.selectFirst(".hero-en")?.text().orEmpty(),
                desc = h.selectFirst(".hero-desc")?.let { textWithBreaks(it) }
            )
        } ?: HeroBlock("", "", "", null)

        val tabs = sections.map { s ->
            TabInfo(
                id = s.id,
                label = navLabels[s.id] ?: s.title,
                num = s.num,
                title = s.title,
                en = s.en
            )
        }

        return SiteContent(notices, tabs, hero, sections)
    }

    fun parsePage(html: String): ParsedPage {
        val doc = Jsoup.parse(html)
        val heroEl = doc.selectFirst("main > section.hero")
        val hero = heroEl?.let { h ->
            HeroBlock(
                eyebrow = h.selectFirst(".eyebrow")?.text().orEmpty(),
                title = h.selectFirst("h1")?.text().orEmpty(),
                en = h.selectFirst(".hero-en")?.text().orEmpty(),
                desc = h.selectFirst(".hero-desc")?.let { textWithBreaks(it) }
            )
        }
        val sections = doc.select("main > section.sec").map { parseSection(it) }
        return ParsedPage(
            title = hero?.title ?: doc.title(),
            hero = hero,
            sections = sections
        )
    }

    private fun parseSection(sec: Element): SectionBlock {
        val id = sec.id()
        val num = sec.selectFirst(".sec-num")?.text().orEmpty()
        val h2 = sec.selectFirst(".sec-head h2")
        val (title, en) = h2Parts(h2)
        val content = sec.selectFirst(".sec-body .content") ?: sec
        val blocks = parseBlocks(content)
        return SectionBlock(id, num, title, en, blocks)
    }

    private fun h2Parts(h2: Element?): Pair<String, String> {
        if (h2 == null) return "" to ""
        val clone = h2.clone()
        clone.selectFirst("span.en")?.remove()
        val title = clone.text().trim()
        val en = h2.selectFirst("span.en")?.text().orEmpty()
        return title to en
    }

    private fun parseBlocks(content: Element): List<Block> {
        return content.children().mapNotNull { el ->
            when {
                el.tagName() == "p" -> Block.Para(textWithBreaks(el))
                el.hasClass("list") -> Block.ListBlock(
                    el.children().filter { it.hasClass("list-item") }.map { item ->
                        ListItemData(
                            n = item.selectFirst(".n")?.text().orEmpty(),
                            t = item.selectFirst(".t")?.text().orEmpty(),
                            d = item.selectFirst(".d")?.text()?.takeIf { it.isNotBlank() },
                            link = item.selectFirst("a[href]")?.attr("href")?.takeIf { it.isNotBlank() }
                        )
                    }
                )
                el.hasClass("courses") -> Block.CoursesBlock(
                    el.children().filter { it.hasClass("course") }.map { c ->
                        CourseData(
                            idx = c.selectFirst(".idx")?.text().orEmpty(),
                            title = c.selectFirst("h3")?.text().orEmpty(),
                            en = c.selectFirst(".en")?.text().orEmpty(),
                            desc = c.selectFirst("p")?.let { textWithBreaks(it) }.orEmpty()
                        )
                    }
                )
                el.hasClass("spec-table") -> {
                    val (headers, rows) = parseTable(el)
                    Block.TableBlock(headers, rows)
                }
                else -> null
            }
        }
    }

    private fun parseTable(table: Element): Pair<List<String>, List<TableRowData>> {
        val headers = table.selectFirst("thead")?.select("th")?.map { it.text().trim() }.orEmpty()
        val rows = table.selectFirst("tbody")?.select("tr")?.mapNotNull { tr ->
            val cells = tr.select("td")
            if (cells.isEmpty()) return@mapNotNull null
            val first = cells[0]
            val title: String
            val count: String?
            if (first.hasClass("pos")) {
                title = first.ownText().trim()
                count = first.selectFirst(".count")?.text()?.trim()?.takeIf { it.isNotBlank() }
            } else {
                title = first.text().trim()
                count = null
            }
            val rest = cells.drop(1).map { td ->
                (td.attr("data-label").trim()) to textWithBreaks(td)
            }
            TableRowData(title, count, rest)
        }.orEmpty()
        return headers to rows
    }

    /** 取元素文字，<br> 换成换行 */
    private fun textWithBreaks(el: Element): String {
        val clone = el.clone()
        clone.select("br").forEach { it.after("\n") }
        return clone.wholeText().trim()
    }
}
