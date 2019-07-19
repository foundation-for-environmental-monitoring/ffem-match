package io.ffem.lite.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
abstract class ResultDao {

    @Query("SELECT * FROM results ORDER BY date DESC")
    abstract fun getResults(): List<TestResult>

    @Query("SELECT * FROM results WHERE id = :id")
    abstract fun getResult(id: String?): TestResult

    @Insert(onConflict = REPLACE)
    abstract fun insert(result: TestResult)

}