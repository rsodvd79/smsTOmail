package it.drhack.smstomail

import androidx.room.TypeConverter

class FilterTypeConverter {
    @TypeConverter
    fun fromFilterType(filterType: FilterType): Int {
        return when (filterType) {
            FilterType.INCLUDE -> 0
            FilterType.EXCLUDE -> 1
        }
    }

    @TypeConverter
    fun toFilterType(value: Int): FilterType {
        return when (value) {
            0 -> FilterType.INCLUDE
            1 -> FilterType.EXCLUDE
            else -> FilterType.INCLUDE // Valore predefinito
        }
    }
}
