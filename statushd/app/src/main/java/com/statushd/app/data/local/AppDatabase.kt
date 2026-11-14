package com.statushd.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaItemEntity::class,
        CompressionJobEntity::class,
        SplitPartEntity::class,
        EditProjectEntity::class,
        TextOverlayEntity::class,
        StickerInstanceEntity::class,
        CreationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun jobDao(): JobDao
    abstract fun splitPartDao(): SplitPartDao
    abstract fun editDao(): EditDao
    abstract fun creationDao(): CreationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "statushd.db",
                ).build().also { instance = it }
            }
        }
    }
}
