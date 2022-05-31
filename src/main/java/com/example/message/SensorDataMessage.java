package com.example.message;

public class SensorDataMessage extends SimpleMissage {

    private int value; // from the sensor

    public SensorDataMessage(String key, int value) {
        super(key);
        this.value = value;
    }

    public String getKey() {
        return super.getKey();
    }

    public int getValue() {
        return value;
    }
}
