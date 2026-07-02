package com.eclipse.webnovel.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eclipse.webnovel.data.db.DownloadStatusEntity
import com.eclipse.webnovel.data.db.DownloadedChapterEntity
import com.eclipse.webnovel.data.db.WebNovelDatabase
import com.eclipse.webnovel.data.source.RoyalRoadSource
import kotlinx.coroutines.delay

/** Downloads every chapter of a novel into the local cache, updating progress in Room. */
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val novelUrl = inputData.getString(KEY_NOVEL_URL) ?: return Result.failure()
        val dao = WebNovelDatabase.get(applicationContext).downloadDao()
        val source = RoyalRoadSource()
        return try {
            val detail = source.detail(novelUrl)
            val total = detail.chapters.size
            val title = detail.summary.title
            val cover = detail.summary.coverUrl
            dao.putStatus(DownloadStatusEntity(novelUrl, title, cover, total, 0, STATE_DOWNLOADING))
            detail.chapters.forEachIndexed { index, chapter ->
                if (dao.chapter(chapter.url) == null) {
                    val content = source.chapter(chapter.url)
                    dao.putChapter(
                        DownloadedChapterEntity(
                            chapterUrl = chapter.url,
                            novelUrl = novelUrl,
                            title = content.title.ifBlank { chapter.title },
                            orderIndex = index,
                            content = content.paragraphs.joinToString("\n\n"),
                        ),
                    )
                    delay(250) // be polite to the source
                }
                dao.putStatus(DownloadStatusEntity(novelUrl, title, cover, total, index + 1, STATE_DOWNLOADING))
            }
            dao.putStatus(DownloadStatusEntity(novelUrl, title, cover, total, total, STATE_DONE))
            Result.success()
        } catch (t: Throwable) {
            dao.status(novelUrl)?.let { dao.putStatus(it.copy(state = STATE_FAILED)) }
            Result.retry()
        }
    }

    companion object {
        const val KEY_NOVEL_URL = "novel_url"
        const val STATE_DOWNLOADING = "DOWNLOADING"
        const val STATE_DONE = "DONE"
        const val STATE_FAILED = "FAILED"
    }
}
