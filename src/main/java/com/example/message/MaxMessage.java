package com.example.message;

public class MaxMessage extends SimpleMissage {

    private double value;

    public MaxMessage(String key, double value) {
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
