package com.eclipse.webnovel.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putChapter(chapter: DownloadedChapterEntity)

    @Query("SELECT * FROM downloaded_chapters WHERE chapterUrl = :url")
    suspend fun chapter(url: String): DownloadedChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putStatus(status: DownloadStatusEntity)

    @Query("SELECT * FROM download_status WHERE novelUrl = :url")
    suspend fun status(url: String): DownloadStatusEntity?

    @Query("SELECT * FROM download_status ORDER BY title")
    fun observeStatuses(): Flow<List<DownloadStatusEntity>>

    @Query("SELECT COALESCE(SUM(LENGTH(content)), 0) FROM downloaded_chapters")
    fun observeTotalBytes(): Flow<Long>

    @Query("DELETE FROM downloaded_chapters WHERE novelUrl = :url")
    suspend fun deleteChapters(url: String)

    @Query("DELETE FROM download_status WHERE novelUrl = :url")
    suspend fun deleteStatus(url: String)
}
