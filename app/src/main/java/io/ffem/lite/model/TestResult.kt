package io.ffem.lite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "results")
data class TestResult(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "name") val name: String
)
