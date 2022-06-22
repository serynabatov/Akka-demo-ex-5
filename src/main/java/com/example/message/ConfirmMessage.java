package com.example.message;

import java.io.Serializable;

public class ConfirmMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public final long deliveryId;

    public ConfirmMessage(long deliveryId) {
        this.deliveryId = deliveryId;
    }

}
