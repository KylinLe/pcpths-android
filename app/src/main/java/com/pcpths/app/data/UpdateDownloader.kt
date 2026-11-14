package com.pcpths.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 更新 APK 下载器：
 * - 下载地址固定走官网本站（国内 github.com 访问不稳，绝不走 github）
 * - 下载到 App 私有目录，通过 FileProvider 交给系统安装器安装
 */
object UpdateDownloader {

    const val APK_URL = "https://kylinle.github.io/pcpths/downloads/pcpths-android.apk"
    const val APK_FILE_NAME = "pcpths-android.apk"
    private const val AUTHORITY = "com.pcpths.app.fileprovider"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun apkFile(context: Context): File =
        File(context.filesDir, "update/$APK_FILE_NAME")

    /** 下载最新 APK 到 App 私有目录（覆盖旧文件），返回文件；onProgress 0..100 */
    suspend fun download(context: Context, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val out = apkFile(context)
            out.parentFile?.mkdirs()
            if (out.exists()) out.delete()
            val request = Request.Builder()
                .url(APK_URL)
                .header("User-Agent", "PCPTHS-Android/1.0")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("空响应")
                val total = body.contentLength()
                var read = 0L
                var lastPct = -1
                body.byteStream().use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            read += n
                            if (total > 0) {
                                val pct = (read * 100 / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
            }
            out
        }

    /** 交给系统安装器安装 */
    fun installIntent(context: Context, file: File): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(context, AUTHORITY, file),
                "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
