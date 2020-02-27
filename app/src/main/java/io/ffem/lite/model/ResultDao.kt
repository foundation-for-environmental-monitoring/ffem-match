package io.ffem.lite.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
abstract class ResultDao {

    @Query("SELECT * FROM results WHERE value = ''")
    abstract fun getPendingResults(): List<TestResult>

    @Query("SELECT * FROM results ORDER BY date DESC")
    abstract fun getResults(): List<TestResult>

    @Query("SELECT * FROM results WHERE id = :id")
    abstract fun getResult(id: String?): TestResult?

    @Insert(onConflict = REPLACE)
    abstract fun insert(result: TestResult)

    @Query("UPDATE results SET name= :name, value = :result WHERE id = :id")
    abstract fun updateResult(id: String, name: String, result: String)

    @Query("DELETE FROM results")
    abstract fun deleteAll()
}