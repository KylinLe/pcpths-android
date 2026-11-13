package com.pcpths.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 官网同步器：
 * - 启动 / 手动刷新时，把官网的小页面抓取到手机本地（filesDir/site/）
 * - 之后所有展示都读本地，不联网
 */
object SiteSync {

    const val BASE_URL = "https://kylinle.github.io/pcpths/"
    private const val HOST = "kylinle.github.io"
    private const val SITE_PREFIX = "/pcpths/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class SyncOutcome(val ok: Boolean, val message: String)

    /** 同步官网：首页 + 同站点的 html 页面（最多两层链接） */
    suspend fun sync(cacheDir: File): SyncOutcome = withContext(Dispatchers.IO) {
        try {
            val siteDir = File(cacheDir, "site")
            val queue = ArrayDeque<Pair<String, Int>>()
            queue.addLast("index.html" to 0)
            val seen = mutableSetOf<String>()
            var fetched = 0
            var failed = 0

            while (queue.isNotEmpty()) {
                val (path, depth) = queue.removeFirst()
                if (!seen.add(path)) continue
                val html = fetch(path)
                if (html == null) {
                    if (path == "index.html") failed++
                    continue
                }
                val file = File(siteDir, path)
                file.parentFile?.mkdirs()
                file.writeText(html)
                fetched++
                if (depth < 2) {
                    for (link in extractSameOriginHtmlLinks(html, path)) {
                        queue.addLast(link to (depth + 1))
                    }
                }
            }

            if (fetched == 0) {
                SyncOutcome(false, "无法连接官网，请检查网络")
            } else if (failed > 0) {
                SyncOutcome(false, "官网部分页面获取失败")
            } else {
                SyncOutcome(true, "官网内容已更新")
            }
        } catch (e: Exception) {
            SyncOutcome(false, "同步出错：${e.message ?: "网络异常"}")
        }
    }

    /** 读取本地缓存的首页 */
    fun loadCache(cacheDir: File): String? =
        File(cacheDir, "site/index.html").takeIf { it.exists() }?.readText()

    /** 读取本地缓存的某个页面 */
    fun loadPage(cacheDir: File, path: String): String? =
        File(cacheDir, "site/$path").takeIf { it.exists() }?.readText()

    private fun fetch(path: String): String? {
        return try {
            val request = Request.Builder()
                .url(BASE_URL + path)
                .header("User-Agent", "PCPTHS-Android/1.0")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 收集同站点的 html 链接（相对当前页面路径解析） */
    private fun extractSameOriginHtmlLinks(html: String, fromPath: String): List<String> {
        val doc = Jsoup.parse(html)
        val base = URI(BASE_URL).resolve(fromPath)
        return doc.select("a[href]").mapNotNull { a ->
            val href = a.attr("href")
            if (href.isBlank() || href.startsWith("#")) return@mapNotNull null
            val clean = href.substringBefore("#").substringBefore("?")
            val resolved = try {
                base.resolve(clean)
            } catch (e: Exception) {
                return@mapNotNull null
            }
            if (resolved.host != null && resolved.host != HOST) return@mapNotNull null
            if (!resolved.path.startsWith(SITE_PREFIX)) return@mapNotNull null
            if (!resolved.path.endsWith(".html")) return@mapNotNull null
            resolved.path.removePrefix(SITE_PREFIX)
        }.distinct()
    }
}
