package io.ffem.lite.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
abstract class ResultDao {

    @Query("SELECT * FROM TestResult ORDER BY date DESC")
    abstract fun getResults(): List<TestResult>

    @Query("SELECT * FROM TestResult WHERE id = :id")
    abstract fun getResult(id: String?): TestResult?

    @Insert(onConflict = REPLACE)
    abstract fun insert(result: TestResult)

    @Insert(onConflict = REPLACE)
    abstract fun insertCalibration(calibration: Calibration)

    @Query("SELECT * FROM Calibration WHERE id = :id")
    abstract fun getCalibration(id: String?): Calibration?

    @Query("UPDATE TestResult SET name = :name, value = :result, valueGrayscale = :resultGrayscale, marginOfError = :marginOfError, error = :error WHERE id = :id")
    abstract fun updateResult(
        id: String,
        name: String,
        result: Double,
        resultGrayscale: Double,
        marginOfError: Double,
        error: Int
    )

    @Query("DELETE FROM TestResult")
    abstract fun deleteAll()

    @Query("SELECT COUNT(*) FROM TestResult")
    abstract fun getCount(): Int

    @Query("SELECT * FROM TestResult ORDER BY date ASC LIMIT 1")
    abstract fun getOldestResult(): TestResult

    @Query("DELETE FROM TestResult WHERE id = :id")
    abstract fun deleteResult(id: String?)
}