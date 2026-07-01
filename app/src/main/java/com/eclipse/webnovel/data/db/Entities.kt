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
