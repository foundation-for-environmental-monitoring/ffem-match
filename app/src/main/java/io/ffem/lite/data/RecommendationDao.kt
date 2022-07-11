package io.ffem.lite.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.ffem.lite.entity.RecommendationInfo

@Dao
interface RecommendationDao {

    @Transaction
    @Query("SELECT * FROM recommendationinfo WHERE testId = :uuid AND crop = :crop AND soil = :soil")
    fun getRecommendations(uuid: String?, crop: Int, soil: String): List<RecommendationInfo?>?

}