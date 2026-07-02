package com.eclipse.webnovel.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStateDao {

    @Query("SELECT * FROM reading_state")
    fun observeAll(): Flow<List<ReadingStateEntity>>

    @Query("SELECT * FROM reading_state WHERE novelUrl = :url")
    fun observe(url: String): Flow<ReadingStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReadingStateEntity)
}
