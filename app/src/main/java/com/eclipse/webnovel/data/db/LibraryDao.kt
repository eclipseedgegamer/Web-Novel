package com.eclipse.webnovel.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library_novels ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<LibraryNovelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM library_novels WHERE novelUrl = :url)")
    fun isInLibrary(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(novel: LibraryNovelEntity)

    @Query("DELETE FROM library_novels WHERE novelUrl = :url")
    suspend fun delete(url: String)

    @Query("SELECT * FROM library_novels")
    suspend fun allOnce(): List<LibraryNovelEntity>

    @Query("UPDATE library_novels SET lastKnownChapters = :count WHERE novelUrl = :url")
    suspend fun updateKnownChapters(url: String, count: Int)
}
