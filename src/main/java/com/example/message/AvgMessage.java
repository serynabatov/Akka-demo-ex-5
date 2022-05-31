package com.example.message;

public class AvgMessage extends SimpleMissage{

    private double value;

    public AvgMessage(String key, double value) {

        super(key);
        this.value = value;
    }

    public String getKey() {
        return super.getKey();
    }

    public double getValue() {
        return this.value;
    }
}
