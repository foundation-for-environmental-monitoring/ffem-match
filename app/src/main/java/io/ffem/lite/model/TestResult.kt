package io.ffem.lite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "results")
data class TestResult(
    @PrimaryKey val id: String = "",
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "sent") val sent: Long,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "localValue") val localValue: String,
    @ColumnInfo(name = "message") val message: String
)
