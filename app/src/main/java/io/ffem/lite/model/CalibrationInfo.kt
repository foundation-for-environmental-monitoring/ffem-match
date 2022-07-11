package io.ffem.lite.model

import androidx.room.Embedded
import androidx.room.Relation

data class CalibrationInfo(
    @Embedded val details: CalibrationDetail,
    @Relation(
        parentColumn = "calibrationId",
        entityColumn = "calibrationId"
    ) var calibrations: List<Calibration>
)
