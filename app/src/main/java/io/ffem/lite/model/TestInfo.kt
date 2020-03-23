package io.ffem.lite.model

import android.os.Parcel
import android.os.Parcelable

class TestInfo() : Parcelable {

    var name: String? = null

    @Suppress("unused")
    var type: String? = null
    var uuid: String? = null
    var unit: String? = null
    var values: List<CalibrationValue> = ArrayList()

    @Transient
    var result: String = ""

    @Transient
    var fileName: String = ""

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
        type = parcel.readString()
        uuid = parcel.readString()
        unit = parcel.readString()
        result = parcel.readString()!!
        fileName = parcel.readString()!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(type)
        parcel.writeString(uuid)
        parcel.writeString(unit)
        parcel.writeString(result)
        parcel.writeString(fileName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestInfo> {
        override fun createFromParcel(parcel: Parcel): TestInfo {
            return TestInfo(parcel)
        }

        override fun newArray(size: Int): Array<TestInfo?> {
            return arrayOfNulls(size)
        }
    }
}