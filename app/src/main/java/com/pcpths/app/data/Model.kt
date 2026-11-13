package com.pcpths.app.data

/** 官网通知条数据 */
data class Notice(
    val id: String,
    val title: String,
    val more: String,
    val target: String
)

/** 底部导航栏的一个标签（对应官网一个栏目） */
data class TabInfo(
    val id: String,
    val label: String,
    val num: String,
    val title: String,
    val en: String
)

/** 首屏（留住光） */
data class HeroBlock(
    val eyebrow: String,
    val title: String,
    val en: String,
    val desc: String?
)

/** 栏目里的内容块 */
sealed interface Block {
    data class Para(val text: String) : Block
    data class ListBlock(val items: List<ListItemData>) : Block
    data class CoursesBlock(val items: List<CourseData>) : Block
    data class TableBlock(val headers: List<String>, val rows: List<TableRowData>) : Block
}

data class ListItemData(
    val n: String,
    val t: String,
    val d: String?,
    val link: String?
)

data class CourseData(
    val idx: String,
    val title: String,
    val en: String,
    val desc: String
)

data class TableRowData(
    val title: String,
    val count: String?,
    val cells: List<Pair<String, String>>
)

/** 官网的一个栏目 */
data class SectionBlock(
    val id: String,
    val num: String,
    val title: String,
    val en: String,
    val blocks: List<Block>
)

/** 一个网页解析后的结果（首页或计划说明页） */
data class ParsedPage(
    val title: String,
    val hero: HeroBlock?,
    val sections: List<SectionBlock>
)

/** 首页解析结果：通知 + 底栏标签 + 首屏 + 各栏目内容 */
data class SiteContent(
    val notices: List<Notice>,
    val tabs: List<TabInfo>,
    val homeHero: HeroBlock,
    val sections: List<SectionBlock>
)
