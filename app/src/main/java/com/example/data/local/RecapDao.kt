package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecapDao {

    @Query("SELECT * FROM local_recaps ORDER BY timestamp DESC")
    fun getAllRecaps(): Flow<List<LocalRecapEntity>>

    @Query("SELECT * FROM local_recaps WHERE id = :id LIMIT 1")
    suspend fun getRecapById(id: String): LocalRecapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecap(entity: LocalRecapEntity)

    @Query("DELETE FROM local_recaps WHERE id = :id")
    suspend fun deleteRecap(id: String)
}
