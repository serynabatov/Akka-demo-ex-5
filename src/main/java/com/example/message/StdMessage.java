package com.example.message;

public class StdMessage extends SimpleMissage{

    public StdMessage(String key, double value) {
        super(key, value);
    }

    public String getKey() {
        return super.getKey();
    }

    public double getValue() {
        return super.getValue();
    }

}
