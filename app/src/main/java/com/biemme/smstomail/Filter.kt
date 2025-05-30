package com.biemme.smstomail

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FilterType {
    INCLUDE,
    EXCLUDE
}

@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String = "",
    val keyword: String = "",
    val filterType: FilterType = FilterType.INCLUDE
)

