package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CafeDao {
    @Query("SELECT * FROM cafes ORDER BY timestamp DESC")
    fun getAllCafes(): Flow<List<CafeEntity>>

    @Query("SELECT * FROM cafes WHERE id = :id")
    fun getCafeById(id: Int): Flow<CafeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCafe(cafe: CafeEntity): Long

    @Delete
    suspend fun deleteCafe(cafe: CafeEntity)
}
