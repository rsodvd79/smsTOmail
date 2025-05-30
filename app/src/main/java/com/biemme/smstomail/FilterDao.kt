package com.biemme.smstomail

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FilterDao {
    @Query("SELECT * FROM filters")
    suspend fun getAllFilters(): List<Filter>

    @Insert
    suspend fun insertFilter(filter: Filter)

    @Delete
    suspend fun deleteFilter(filter: Filter)
}

