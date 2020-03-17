package io.ffem.lite.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TestInfo() : Parcelable {

    @SerializedName("name")
    @Expose
    var name: String? = null

    @Suppress("unused")
    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("uuid")
    @Expose
    var uuid: String? = null

    @SerializedName("unit")
    @Expose
    var unit: String? = null

    @SerializedName("values")
    @Expose
    var values: List<CalibrationValue> = ArrayList()

    var result: String = ""
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