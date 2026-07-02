package com.eclipse.webnovel.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterUpdateDao {

    @Insert
    suspend fun insert(update: ChapterUpdateEntity)

    @Query("SELECT * FROM chapter_updates ORDER BY foundAt DESC, id DESC")
    fun observeAll(): Flow<List<ChapterUpdateEntity>>

    @Query("SELECT COUNT(*) FROM chapter_updates WHERE seen = 0")
    fun observeUnseenCount(): Flow<Int>

    @Query("UPDATE chapter_updates SET seen = 1 WHERE seen = 0")
    suspend fun markAllSeen()
}
