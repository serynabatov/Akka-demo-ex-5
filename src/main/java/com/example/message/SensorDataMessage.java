package com.example.message;

public class SensorDataMessage {

    private String key;   // it says about the type of data
    private int value; // from the sensor

    public SensorDataMessage(String key, int value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }
}
