package com.eclipse.webnovel.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.eclipse.webnovel.data.db.DownloadStatusEntity
import com.eclipse.webnovel.data.db.DownloadedChapterEntity
import com.eclipse.webnovel.data.db.WebNovelDatabase
import kotlinx.coroutines.flow.Flow

/** Offline chapter cache + download scheduling (WorkManager). */
class DownloadRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = WebNovelDatabase.get(context).downloadDao()

    fun enqueue(novelUrl: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_NOVEL_URL to novelUrl))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("download:$novelUrl", ExistingWorkPolicy.KEEP, request)
    }

    fun observeStatuses(): Flow<List<DownloadStatusEntity>> = dao.observeStatuses()

    fun observeTotalBytes(): Flow<Long> = dao.observeTotalBytes()

    suspend fun cachedChapter(url: String): DownloadedChapterEntity? = dao.chapter(url)

    suspend fun delete(novelUrl: String) {
        dao.deleteChapters(novelUrl)
        dao.deleteStatus(novelUrl)
    }
}
