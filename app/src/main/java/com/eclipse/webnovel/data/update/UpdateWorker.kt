package com.eclipse.webnovel.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eclipse.webnovel.data.db.ChapterUpdateEntity
import com.eclipse.webnovel.data.db.WebNovelDatabase
import com.eclipse.webnovel.data.source.SourceRegistry
import kotlinx.coroutines.delay

/** Periodically re-checks each library novel for new chapters and records them. */
class UpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = WebNovelDatabase.get(applicationContext)
        val libraryDao = db.libraryDao()
        val updateDao = db.chapterUpdateDao()
        var newCount = 0
        for (novel in libraryDao.allOnce()) {
            try {
                val detail = SourceRegistry.sourceFor(novel.novelUrl).detail(novel.novelUrl)
                val total = detail.chapters.size
                if (novel.lastKnownChapters in 1 until total) {
                    val now = System.currentTimeMillis()
                    detail.chapters.subList(novel.lastKnownChapters, total).forEach { chapter ->
                        updateDao.insert(
                            ChapterUpdateEntity(
                                novelUrl = novel.novelUrl,
                                novelTitle = novel.title,
                                chapterUrl = chapter.url,
                                chapterTitle = chapter.title,
                                foundAt = now,
                                seen = false,
                            ),
                        )
                        newCount++
                    }
                }
                libraryDao.updateKnownChapters(novel.novelUrl, total)
                delay(500)
            } catch (_: Throwable) {
                // Skip this novel; the next cycle retries.
            }
        }
        if (newCount > 0) Notifications.postNewChapters(applicationContext, newCount)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "chapter-update-check"
    }
}
