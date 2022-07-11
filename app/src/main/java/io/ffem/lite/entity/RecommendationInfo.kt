package io.ffem.lite.entity

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(primaryKeys = ["id"])
data class RecommendationInfo(
    var id: Long = 0,
    var testId: String = "",
    val crop: Int = 0,
    val soil: String = "",
    val risk0: Double?,
    val risk1: Double?,
    val risk2: Double?, // Low
    val risk3: Double?, // Med
    val risk4: Double?, // High
    val risk5: Double?,
    val risk6: Double?
) : Parcelable