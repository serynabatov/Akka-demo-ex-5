package com.example.message;

public class StdMessage extends SimpleMissage{

    private double value;

    public StdMessage(String key, double value) {
        super(key);
        this.value = value;
    }

    public String getKey() {
        return super.getKey();
    }

    public double getValue() {
        return value;
    }

}
