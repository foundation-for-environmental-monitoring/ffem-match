package io.ffem.lite.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
abstract class ResultDao {

    @Query("SELECT * FROM results WHERE status = 0 LIMIT 1")
    abstract fun getUnsent(): List<TestResult>

    @Query("SELECT * FROM results WHERE status = 1")
    abstract fun getPendingResults(): List<TestResult>

    @Query("SELECT * FROM results WHERE localValue = ''")
    abstract fun getPendingLocalResults(): List<TestResult>

    @Query("SELECT * FROM results ORDER BY date DESC")
    abstract fun getResults(): List<TestResult>

    @Query("SELECT * FROM results WHERE id = :id")
    abstract fun getResult(id: String?): TestResult?

    @Insert(onConflict = REPLACE)
    abstract fun insert(result: TestResult)

    @Query("UPDATE results SET status = :status, sent = :sentDate, message = :message WHERE id = :id")
    abstract fun updateStatus(id: String, status: Int, sentDate: Long, message: String)

    @Query("UPDATE results SET status = :status, message = :message, value = :result WHERE id = :id")
    abstract fun updateResult(id: String, status: Int, message: String, result: String)

    @Query("UPDATE results SET localValue = :result WHERE id = :id")
    abstract fun updateLocalResult(id: String, result: String)

    @Query("DELETE FROM results")
    abstract fun deleteAll()

    @Query("UPDATE results SET status = 1, message = 'Analyzing', value = ''")
    abstract fun reset()

}