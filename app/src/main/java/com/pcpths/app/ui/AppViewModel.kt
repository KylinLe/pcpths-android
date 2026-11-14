package com.pcpths.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pcpths.app.data.AppRepository
import com.pcpths.app.data.ParsedPage

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)

    val state = repo.state

    init {
        repo.start()
    }

    fun refresh() = repo.refresh()
    fun dismissNotice(id: String) = repo.dismissNotice(id)
    fun reshowNotices() = repo.reshowNotices()
    fun consumeMessage() = repo.consumeMessage()
    suspend fun parsePage(path: String): ParsedPage? = repo.parsePage(path)
    fun downloadUpdate() = repo.downloadUpdate()
    fun consumeUpdateReady() = repo.consumeUpdateReady()
}
