package com.eclipse.webnovel.data.library

import android.content.Context
import com.eclipse.webnovel.data.db.ChapterUpdateEntity
import com.eclipse.webnovel.data.db.LibraryNovelEntity
import com.eclipse.webnovel.data.db.ReadingStateEntity
import com.eclipse.webnovel.data.db.WebNovelDatabase
import com.eclipse.webnovel.data.model.ChapterRef
import com.eclipse.webnovel.data.model.NovelDetail
import kotlinx.coroutines.flow.Flow

/** The user's saved novels and where they last were in each. */
class LibraryRepository(context: Context) {

    private val db = WebNovelDatabase.get(context)
    private val dao = db.libraryDao()
    private val stateDao = db.readingStateDao()
    private val updateDao = db.chapterUpdateDao()

    fun observeLibrary(): Flow<List<LibraryNovelEntity>> = dao.observeAll()

    fun isInLibrary(novelUrl: String): Flow<Boolean> = dao.isInLibrary(novelUrl)

    fun observeAllStates(): Flow<List<ReadingStateEntity>> = stateDao.observeAll()

    fun observeState(novelUrl: String): Flow<ReadingStateEntity?> = stateDao.observe(novelUrl)

    suspend fun recordReading(novelUrl: String, chapter: ChapterRef, index: Int, total: Int) {
        stateDao.upsert(
            ReadingStateEntity(
                novelUrl = novelUrl,
                lastChapterUrl = chapter.url,
                lastChapterTitle = chapter.title,
                chapterIndex = index,
                totalChapters = total,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun add(detail: NovelDetail) {
        dao.insert(
            LibraryNovelEntity(
                novelUrl = detail.summary.url,
                sourceId = detail.summary.sourceId,
                title = detail.summary.title,
                coverUrl = detail.summary.coverUrl,
                author = detail.author,
                status = detail.status,
                addedAt = System.currentTimeMillis(),
                lastKnownChapters = detail.chapters.size,
            ),
        )
    }

    suspend fun remove(novelUrl: String) = dao.delete(novelUrl)

    fun observeUpdates(): Flow<List<ChapterUpdateEntity>> = updateDao.observeAll()

    fun observeUnseenCount(): Flow<Int> = updateDao.observeUnseenCount()

    suspend fun markUpdatesSeen() = updateDao.markAllSeen()
}
