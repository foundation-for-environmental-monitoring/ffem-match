package io.ffem.lite.model;

public class ColorCompareInfo {

    private final int resultColor;
    private final int matchedColor;
    private final double distance;
    private final double result;

    public ColorCompareInfo(double result, int resultColor, int matchedColor, double distance) {
        this.result = result;
        this.resultColor = resultColor;
        this.matchedColor = matchedColor;
        this.distance = distance;
    }

    public double getResult() {
        return result;
    }

    @SuppressWarnings("unused")
    public int getResultColor() {
        return resultColor;
    }

    public int getMatchedColor() {
        return matchedColor;
    }

    public double getDistance() {
        return distance;
    }
}
