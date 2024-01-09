package io.ffem.lite.data

import androidx.room.*
import io.ffem.lite.model.Calibration
import io.ffem.lite.model.CalibrationDetail
import io.ffem.lite.model.CalibrationInfo

@Dao
interface CalibrationDao {

    @Transaction
    @Query("SELECT * FROM calibrationdetail WHERE testId = :uuid and isCurrent = 1")
    fun getCalibrations(uuid: String?): CalibrationInfo?

    @Transaction
    @Query("SELECT * FROM calibrationdetail WHERE calibrationId = :calibrationId")
    fun getCalibrations(calibrationId: Long): CalibrationInfo?

    @Transaction
    @Query("SELECT * FROM calibrationdetail WHERE testId = :uuid and isCurrent != 1")
    fun getAllCalibrations(uuid: String?): List<CalibrationInfo?>?

    @Transaction
    @Query("SELECT * FROM calibrationdetail WHERE testId = :uuid and isCurrent != 1")
    fun getCalibrationInfo(uuid: String?): CalibrationInfo?

    @Query("SELECT * FROM calibrationdetail WHERE testId = :uuid and isCurrent = 1")
    fun getCalibrationDetail(uuid: String?): CalibrationDetail?

    @Query("SELECT calibrationId FROM calibrationdetail WHERE  testId = :uuid and isCurrent = 1")
    fun getCurrentCalibrationId(uuid: String?): Long

    @Query("SELECT name FROM calibrationdetail WHERE  colors = :colors and isCurrent != 1")
    fun getCalibrationColorString(colors: String?): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(calibration: Calibration)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(calibrationDetail: CalibrationDetail): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(calibrations: List<Calibration>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(calibrationDetail: CalibrationDetail)

    @Update
    fun update(calibration: Calibration)

    @Query("DELETE FROM calibrationdetail WHERE calibrationId = :calibrationId")
    fun deleteDetail(calibrationId: Long)

    @Query("DELETE FROM calibration WHERE calibrationId = :calibrationId")
    fun deleteCalibrations(calibrationId: Long)

    @Query("SELECT name FROM calibrationdetail WHERE name = :name")
    fun getCalibrationByName(name: String): String?
}