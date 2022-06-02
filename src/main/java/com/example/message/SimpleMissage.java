package com.example.message;

import java.io.Serializable;

public class SimpleMissage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;

    public SimpleMissage(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

}
