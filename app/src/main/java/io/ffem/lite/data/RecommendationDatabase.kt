package io.ffem.lite.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import de.ueen.roomassethelper.RoomAssetHelper
import io.ffem.lite.entity.RecommendationInfo

@Database(
    entities = [RecommendationInfo::class],
    version = 1,
    exportSchema = false
)
abstract class RecommendationDatabase : RoomDatabase() {
    abstract fun recommendationDao(): RecommendationDao

    companion object {
        private lateinit var INSTANCE: RecommendationDatabase
        fun getRecommendationDatabase(context: Context): RecommendationDatabase {
            synchronized(RecommendationDatabase::class) {

                INSTANCE = RoomAssetHelper.databaseBuilder(
                    context.applicationContext,
                    RecommendationDatabase::class.java,
                    "Recommendation.db",
                    version = 2
                )
                    .allowMainThreadQueries()
                    .createFromAsset("Recommendation.db")
                    .build()
            }
            return INSTANCE
        }
    }
}