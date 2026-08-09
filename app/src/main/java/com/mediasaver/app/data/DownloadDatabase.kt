package com.mediasaver.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class MediaKind { VIDEO, AUDIO, IMAGE }
enum class DownloadStatus { QUEUED, RUNNING, DONE, FAILED }

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUrl: String,
    val platform: String,
    val title: String,
    val filePath: String?,
    val thumbnailUrl: String?,
    val kind: MediaKind,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DownloadDao {
    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}

class Converters {
    @TypeConverter
    fun fromKind(v: MediaKind) = v.name
    @TypeConverter
    fun toKind(v: String) = MediaKind.valueOf(v)
    @TypeConverter
    fun fromStatus(v: DownloadStatus) = v.name
    @TypeConverter
    fun toStatus(v: String) = DownloadStatus.valueOf(v)
}
