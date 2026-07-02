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
    val lastKnownChapters: Int = 0,
)

/** A new chapter found for a library novel by the background update check. */
@Entity(tableName = "chapter_updates")
data class ChapterUpdateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelUrl: String,
    val novelTitle: String,
    val chapterUrl: String,
    val chapterTitle: String,
    val foundAt: Long,
    val seen: Boolean,
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

/** A chapter cached for offline reading. `content` = paragraphs joined by blank lines. */
@Entity(tableName = "downloaded_chapters")
data class DownloadedChapterEntity(
    @PrimaryKey val chapterUrl: String,
    val novelUrl: String,
    val title: String,
    val orderIndex: Int,
    val content: String,
)

/** Per-novel download progress for the Saved screen. */
@Entity(tableName = "download_status")
data class DownloadStatusEntity(
    @PrimaryKey val novelUrl: String,
    val title: String,
    val coverUrl: String?,
    val total: Int,
    val done: Int,
    val state: String,
)
