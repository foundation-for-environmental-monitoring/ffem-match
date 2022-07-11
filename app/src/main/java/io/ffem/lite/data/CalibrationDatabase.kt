package io.ffem.lite.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.ffem.lite.model.Calibration
import io.ffem.lite.model.CalibrationDetail

fun clearData(context: Context) {
    val db = AppDatabase.getDatabase(context)
    try {
        db.clearAllTables()
    } finally {
        db.close()
    }
}

@Database(
    entities = [Calibration::class, CalibrationDetail::class],
    version = 1,
    exportSchema = false
)
abstract class CalibrationDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrationDao

    companion object {
        private lateinit var INSTANCE: CalibrationDatabase
        fun getDatabase(context: Context): CalibrationDatabase {
            synchronized(CalibrationDatabase::class) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    CalibrationDatabase::class.java, "calibration.db"
                ).allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return INSTANCE
        }
    }
}