package com.example;

public class AvgMessage {

    private String key;
    private double value;

    public AvgMessage(String key, double value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public double getValue() {
        return value;
    }
}
