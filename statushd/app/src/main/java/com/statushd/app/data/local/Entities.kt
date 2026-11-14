package com.statushd.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Jadual-jadual ini cermin replica/schema.sql (7 jadual).
// Status kerja: pending -> running -> delivered | failed.

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val type: String, // "video" | "photo"
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val dateAdded: Long = 0,
)

@Entity(
    tableName = "compression_jobs",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaId"), Index("status")],
)
data class CompressionJobEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val status: String = "pending", // pending | running | delivered | failed
    val preset: String = "720p", // 720p | 1080p | 4k
    val aspect: String = "original", // original | 9:16
    val qualityFhd: Int = 0,
    val metaWidth: Int = 0,
    val metaHeight: Int = 0,
    val metaRotation: Int = 0,
    val metaHdr: Int = 0,
    val clipLengthMs: Long = 0,
    val durationMs: Long = 0,
    val progress: Int = 0,
    val outputUri: String? = null,
    val sizeBefore: Long = 0,
    val sizeAfter: Long = 0,
    val caption: String? = null,
    val senderNumber: String? = null,
    val whatsapp: String? = null,
    val seq: Int = 0,
    val error: String? = null,
    val createdAt: Long = 0,
)

@Entity(
    tableName = "split_parts",
    foreignKeys = [
        ForeignKey(
            entity = CompressionJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["jobId", "partIndex"], unique = true)],
)
data class SplitPartEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val partIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val outputUri: String? = null,
    val status: String = "pending",
)

@Entity(
    tableName = "edit_projects",
    foreignKeys = [
        ForeignKey(
            entity = CompressionJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EditProjectEntity(
    @PrimaryKey val jobId: String,
    val trimStartMs: Long = 0,
    val trimEndMs: Long? = null,
    val musicUri: String? = null,
    val musicTitle: String? = null,
)

@Entity(
    tableName = "text_overlays",
    foreignKeys = [
        ForeignKey(
            entity = CompressionJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class TextOverlayEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val text: String,
    val font: String = "default",
    val sizeSp: Float = 24f,
    val color: Int = -1,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val rotation: Float = 0f,
    val glow: Int = 0,
    val border: Int = 0,
    val shadow: Int = 0,
)

@Entity(
    tableName = "sticker_instances",
    foreignKeys = [
        ForeignKey(
            entity = CompressionJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class StickerInstanceEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val stickerId: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

@Entity(tableName = "creations")
data class CreationEntity(
    @PrimaryKey val id: String,
    val tool: String,
    val inputUri: String,
    val outputUri: String,
    val createdAt: Long = 0,
)

@Dao
interface MediaItemDao {
    @Insert
    suspend fun insert(item: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItemEntity?
}

@Dao
interface JobDao {
    @Insert
    suspend fun insert(job: CompressionJobEntity)

    @Update
    suspend fun update(job: CompressionJobEntity)

    @Query("SELECT * FROM compression_jobs WHERE id = :id")
    fun observeById(id: String): Flow<CompressionJobEntity?>

    @Query("SELECT * FROM compression_jobs WHERE id = :id")
    suspend fun getById(id: String): CompressionJobEntity?

    @Query("SELECT * FROM compression_jobs WHERE status = 'delivered' ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentDelivered(limit: Int = 6): Flow<List<CompressionJobEntity>>

    @Query("UPDATE compression_jobs SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, progress: Int)

    @Query("UPDATE compression_jobs SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query("UPDATE compression_jobs SET status = :status, outputUri = :outputUri, sizeAfter = :sizeAfter, progress = 100 WHERE id = :id")
    suspend fun markDelivered(id: String, status: String, outputUri: String, sizeAfter: Long)

    @Query("UPDATE compression_jobs SET status = 'failed', error = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("DELETE FROM compression_jobs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SplitPartDao {
    @Insert
    suspend fun insertAll(parts: List<SplitPartEntity>)

    @Update
    suspend fun update(part: SplitPartEntity)

    @Query("SELECT * FROM split_parts WHERE jobId = :jobId ORDER BY partIndex")
    fun observeByJob(jobId: String): Flow<List<SplitPartEntity>>

    @Query("UPDATE split_parts SET status = :status, outputUri = :outputUri WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, outputUri: String?)
}

@Dao
interface EditDao {
    @Insert
    suspend fun insert(project: EditProjectEntity)

    @Query("SELECT * FROM edit_projects WHERE jobId = :jobId")
    suspend fun getByJob(jobId: String): EditProjectEntity?
}

@Dao
interface CreationDao {
    @Query("SELECT * FROM creations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CreationEntity>>
}
