package com.eclipse.webnovel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LibraryNovelEntity::class], version = 1, exportSchema = false)
abstract class WebNovelDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var instance: WebNovelDatabase? = null

        fun get(context: Context): WebNovelDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WebNovelDatabase::class.java,
                    "webnovel.db",
                ).build().also { instance = it }
            }
    }
}
