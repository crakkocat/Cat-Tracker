package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "cats")
data class Cat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val breed: String,
    val ageMonths: Int, // Age in months for better precision
    val weightKg: Double,
    val avatarEmoji: String, // e.g. "🐱", "🐈", "🦁"
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracking_logs",
    foreignKeys = [
        ForeignKey(
            entity = Cat::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["catId"])]
)
data class TrackingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val catId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // YYYY-MM-DD for grouping
    val type: String, // "DIET", "ACTIVITY", "HEALTH"
    val metricName: String, // e.g. "dry_food", "wet_food", "water", "playtime", "sleep", "weight", "temp", "mood", "notes"
    val valueDouble: Double = 0.0,
    val valueString: String = ""
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Cat::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["catId"])]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val catId: Int,
    val title: String,
    val type: String, // "VACCINATION", "CHECK_UP", "MEDICATION", "OTHER"
    val dueDateString: String, // YYYY-MM-DD
    val notes: String = "",
    val isCompleted: Boolean = false
)

// --- DAO ---

@Dao
interface CatDao {
    @Query("SELECT * FROM cats ORDER BY createdTimestamp DESC")
    fun getAllCatsFlow(): Flow<List<Cat>>

    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun getCatById(id: Int): Cat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCat(cat: Cat): Long

    @Update
    suspend fun updateCat(cat: Cat)

    @Delete
    suspend fun deleteCat(cat: Cat)

    @Query("SELECT * FROM tracking_logs WHERE catId = :catId ORDER BY timestamp DESC")
    fun getLogsForCatFlow(catId: Int): Flow<List<TrackingLog>>

    @Query("SELECT * FROM tracking_logs WHERE catId = :catId AND dateString = :date ORDER BY timestamp DESC")
    fun getDailyLogsForCatFlow(catId: Int, date: String): Flow<List<TrackingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TrackingLog)

    @Delete
    suspend fun deleteLog(log: TrackingLog)
    
    @Query("DELETE FROM tracking_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("SELECT * FROM reminders WHERE catId = :catId ORDER BY dueDateString ASC")
    fun getRemindersForCatFlow(catId: Int): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
}

// --- DATABASE ---

@Database(entities = [Cat::class, TrackingLog::class, Reminder::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cat_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- REPOSITORY ---

class CatRepository(private val catDao: CatDao) {
    val allCats: Flow<List<Cat>> = catDao.getAllCatsFlow()

    suspend fun getCatById(id: Int): Cat? = catDao.getCatById(id)

    suspend fun insertCat(cat: Cat): Long = catDao.insertCat(cat)

    suspend fun updateCat(cat: Cat) = catDao.updateCat(cat)

    suspend fun deleteCat(cat: Cat) = catDao.deleteCat(cat)

    fun getLogsForCat(catId: Int): Flow<List<TrackingLog>> = catDao.getLogsForCatFlow(catId)

    fun getDailyLogsForCat(catId: Int, date: String): Flow<List<TrackingLog>> = 
        catDao.getDailyLogsForCatFlow(catId, date)

    suspend fun insertLog(log: TrackingLog) = catDao.insertLog(log)

    suspend fun deleteLog(log: TrackingLog) = catDao.deleteLog(log)
    
    suspend fun deleteLogById(id: Int) = catDao.deleteLogById(id)

    fun getRemindersForCat(catId: Int): Flow<List<Reminder>> = catDao.getRemindersForCatFlow(catId)

    suspend fun insertReminder(reminder: Reminder): Long = catDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) = catDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) = catDao.deleteReminder(reminder)

    suspend fun deleteReminderById(id: Int) = catDao.deleteReminderById(id)
}
