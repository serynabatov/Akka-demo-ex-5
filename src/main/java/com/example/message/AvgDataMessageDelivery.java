package com.example.message;

import java.io.Serializable;

public class AvgDataMessageDelivery implements Serializable {

    private static final long serialVersionUID = 1L;
    public final long deliveryId;
    public final AvgMessage s;


    public AvgDataMessageDelivery(long deliveryId, AvgMessage s) {
        this.deliveryId = deliveryId;
        this.s = s;
    }

}
