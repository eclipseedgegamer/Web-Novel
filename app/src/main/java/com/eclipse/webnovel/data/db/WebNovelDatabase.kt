package com.eclipse.webnovel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LibraryNovelEntity::class,
        ReadingStateEntity::class,
        DownloadedChapterEntity::class,
        DownloadStatusEntity::class,
        ChapterUpdateEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class WebNovelDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao
    abstract fun readingStateDao(): ReadingStateDao
    abstract fun downloadDao(): DownloadDao
    abstract fun chapterUpdateDao(): ChapterUpdateDao

    companion object {
        @Volatile
        private var instance: WebNovelDatabase? = null

        fun get(context: Context): WebNovelDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WebNovelDatabase::class.java,
                    "webnovel.db",
                )
                    // Pre-release: no real user data yet, so recreate on schema change.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
