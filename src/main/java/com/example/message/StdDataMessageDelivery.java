package com.example.message;

import java.io.Serializable;

public class StdDataMessageDelivery implements Serializable {

    private static final long serialVersionUID = 1L;
    public final long deliveryId;
    public final StdMessage s;


    public StdDataMessageDelivery(long deliveryId, StdMessage s) {
        this.deliveryId = deliveryId;
        this.s = s;
    }

}
