package com.eclipse.webnovel.data.library

import android.content.Context
import com.eclipse.webnovel.data.db.LibraryNovelEntity
import com.eclipse.webnovel.data.db.WebNovelDatabase
import com.eclipse.webnovel.data.model.NovelDetail
import kotlinx.coroutines.flow.Flow

/** The user's saved novels. */
class LibraryRepository(context: Context) {

    private val dao = WebNovelDatabase.get(context).libraryDao()

    fun observeLibrary(): Flow<List<LibraryNovelEntity>> = dao.observeAll()

    fun isInLibrary(novelUrl: String): Flow<Boolean> = dao.isInLibrary(novelUrl)

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
            ),
        )
    }

    suspend fun remove(novelUrl: String) = dao.delete(novelUrl)
}
