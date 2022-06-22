package com.example.message;

import java.io.Serializable;

public class SensorDataMessageDelivery implements Serializable {

    private static final long serialVersionUID = 1L;
    public final long deliveryId;
    public final SensorDataMessage s;

    public SensorDataMessageDelivery(long deliveryId, SensorDataMessage s) {
        this.deliveryId = deliveryId;
        this.s = s;
    }


}
