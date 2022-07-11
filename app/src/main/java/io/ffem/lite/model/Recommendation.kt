package io.ffem.lite.model

data class Recommendation(
    var farmerName: String? = null,
    var phoneNumber: String? = null,
    var villageName: String? = null,
    var sampleNumber: String? = null,
    var geoLocation: String? = null,
    var state: String = "",
    var district: String = "",
    var crop: String = "",
    var soilType: String = "",
    var nitrogenResult: String? = null,
    var phosphorusResult: String? = null,
    var potassiumResult: String? = null,
    var nitrogenRecommendation: String = "",
    var phosphorusRecommendation: String = "",
    var potassiumRecommendation: String = "",
    var nitrogenRisk: String = "",
    var phosphorousRisk: String = "",
    var potassiumRisk: String = "",
    var pH: String? = null
)