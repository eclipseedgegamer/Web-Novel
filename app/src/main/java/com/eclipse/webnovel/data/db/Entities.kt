package com.eclipse.webnovel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A novel the user has saved to their library. */
@Entity(tableName = "library_novels")
data class LibraryNovelEntity(
    @PrimaryKey val novelUrl: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val author: String?,
    val status: String?,
    val addedAt: Long,
)

/** Where the user last was in a novel: the last-opened chapter and its position. */
@Entity(tableName = "reading_state")
data class ReadingStateEntity(
    @PrimaryKey val novelUrl: String,
    val lastChapterUrl: String,
    val lastChapterTitle: String,
    val chapterIndex: Int,
    val totalChapters: Int,
    val updatedAt: Long,
)
