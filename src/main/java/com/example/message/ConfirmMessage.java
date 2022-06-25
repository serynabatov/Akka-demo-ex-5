package com.example.message;

import java.io.Serializable;

public class ConfirmMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public final long deliveryId;
    public final SimpleMissage m;

    public ConfirmMessage(long deliveryId, SimpleMissage m) {

        this.deliveryId = deliveryId;
        this.m = m;
    }

}
