package io.ffem.lite.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Calibration {

    @SerializedName("width")
    @Expose
    public Integer width;
    @SerializedName("height")
    @Expose
    public Integer height;
    @SerializedName("values")
    @Expose
    public List<CalibrationValue> values = null;
}