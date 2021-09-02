package io.ffem.lite.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update

@Dao
interface ResultDao {

    @Query("SELECT * FROM TestResult ORDER BY date DESC")
    fun getResults(): List<TestResult>

    @Query("SELECT * FROM TestResult WHERE id = :id")
    fun getResult(id: String?): TestResult?

    @Insert(onConflict = REPLACE)
    fun insert(result: TestResult)

    @Update(onConflict = REPLACE)
    fun update(result: TestResult)

    @Insert(onConflict = REPLACE)
    fun insertCalibration(calibration: Calibration)

    @Query("SELECT * FROM Calibration WHERE id = :id")
    fun getCalibration(id: String?): Calibration?

    @Query("DELETE FROM Calibration")
    fun deleteCalibration()

    @Query("UPDATE TestResult SET uuid = :uuid, name = :name, sampleType = :sampleType, value = :result, marginOfError = :marginOfError, error = :error WHERE id = :id")
    fun updateResult(
        id: String,
        uuid: String,
        name: String,
        sampleType: String,
        result: Double,
        marginOfError: Double,
        error: Int
    )

    @Query("UPDATE TestResult SET testImageNumber = :testImageNumber WHERE id = :id")
    fun updateResultSampleNumber(id: String, testImageNumber: Int)

    @Query("DELETE FROM TestResult")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM TestResult")
    fun getCount(): Int

    @Query("SELECT * FROM TestResult ORDER BY date ASC LIMIT 1")
    fun getOldestResult(): TestResult

    @Query("DELETE FROM TestResult WHERE id = :id")
    fun deleteResult(id: String?)
}