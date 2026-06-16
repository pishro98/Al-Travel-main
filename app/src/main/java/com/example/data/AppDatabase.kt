package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "travel_plans")
data class TravelPlanEntity(
    @PrimaryKey val id: Int = 1,
    val planData: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_travel_plans")
data class SavedTravelPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destination: String,
    val dates: String,
    val totalBudget: String,
    val planData: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TravelPlanDao {
    @Query("SELECT * FROM travel_plans WHERE id = 1")
    suspend fun getCachedPlan(): TravelPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TravelPlanEntity)

    @Query("SELECT * FROM saved_travel_plans ORDER BY timestamp DESC")
    fun getAllSavedPlans(): kotlinx.coroutines.flow.Flow<List<SavedTravelPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlan(plan: SavedTravelPlanEntity)

    @Query("DELETE FROM saved_travel_plans WHERE id = :id")
    suspend fun deleteSavedPlanById(id: Int)
}

@Database(entities = [TravelPlanEntity::class, SavedTravelPlanEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun travelPlanDao(): TravelPlanDao
    
    companion object {
        @Volatile private var instance: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
    }
}
