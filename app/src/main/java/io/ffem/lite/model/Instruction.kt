package io.ffem.lite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Instruction(
    var name: String? = null,
    var type: String? = null,
    var subtype: TestType? = null,
    var uuid: String? = null,
    var section: ArrayList<String> = ArrayList(),
    var index: Int = 0
) : Parcelable {

//    constructor(value: String, image: String?) : this() {
//        section = java.util.ArrayList()
//        val values = value.split(",".toRegex()).toTypedArray()
//        section.add(values[0])
//        if (values.size > 1) {
//            section.add(values[1])
//        }
//        section.add(image!!)
//    }
}