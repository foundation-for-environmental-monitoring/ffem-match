package io.ffem.lite.data

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import io.ffem.lite.model.CardCalibration

@Dao
interface CardCalibrationDao {

    @Insert(onConflict = REPLACE)
    fun insertCalibration(calibration: CardCalibration)

    @Query("SELECT * FROM CardCalibration WHERE id = :id")
    fun getCalibration(id: String?): CardCalibration?

    @Query("DELETE FROM CardCalibration WHERE id = :id")
    fun deleteCalibration(id: String?)

    @Query("SELECT * FROM TestResult ORDER BY date DESC")
    fun getResults(): LiveData<List<TestResult>>

    @Query("SELECT * FROM TestResult WHERE id = :id")
    fun getResult(id: String?): TestResult?

    @Insert(onConflict = REPLACE)
    fun insert(result: TestResult)

    @Update(onConflict = REPLACE)
    fun update(result: TestResult)

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