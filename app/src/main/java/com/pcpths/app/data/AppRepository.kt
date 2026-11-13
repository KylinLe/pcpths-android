package com.pcpths.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** App 的数据中枢：缓存加载、同步、通知关闭记忆 */
class AppRepository(context: Context) {

    data class UiState(
        val content: SiteContent? = null,
        val syncing: Boolean = false,
        val offline: Boolean = false,
        val message: String? = null,
        val dismissedNotices: Set<String> = emptySet()
    )

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("pcpths_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cacheDir get() = appContext.filesDir

    private val _state = MutableStateFlow(
        UiState(
            dismissedNotices = prefs.getStringSet(KEY_DISMISSED, emptySet())?.toSet()
                ?: emptySet()
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** App 启动：先展示本地缓存（如有），再后台同步一次 */
    fun start() {
        scope.launch {
            val html = SiteSync.loadCache(cacheDir)
            if (html != null) {
                _state.update { it.copy(content = Parser.parseHome(html)) }
            }
            syncInternal(manual = false)
        }
    }

    /** 手动刷新 */
    fun refresh() {
        scope.launch { syncInternal(manual = true) }
    }

    private suspend fun syncInternal(manual: Boolean) {
        if (_state.value.syncing) return
        _state.update { it.copy(syncing = true) }
        val outcome = SiteSync.sync(cacheDir)
        val html = SiteSync.loadCache(cacheDir)
        _state.update {
            it.copy(
                content = html?.let { h -> Parser.parseHome(h) },
                syncing = false,
                offline = !outcome.ok,
                message = if (manual) outcome.message else null
            )
        }
    }

    fun dismissNotice(id: String) {
        val newSet = _state.value.dismissedNotices + id
        prefs.edit().putStringSet(KEY_DISMISSED, newSet).apply()
        _state.update { it.copy(dismissedNotices = newSet) }
    }

    /** 铃铛：重新显示所有已关闭的通知 */
    fun reshowNotices() {
        prefs.edit().remove(KEY_DISMISSED).apply()
        _state.update { it.copy(dismissedNotices = emptySet()) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    /** 解析某个缓存页面（计划说明页） */
    suspend fun parsePage(path: String): ParsedPage? = withContext(Dispatchers.IO) {
        SiteSync.loadPage(cacheDir, path)?.let { Parser.parsePage(it) }
    }

    companion object {
        private const val KEY_DISMISSED = "dismissed_notices"
    }
}
