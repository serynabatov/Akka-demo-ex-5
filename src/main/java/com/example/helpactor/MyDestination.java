package com.example.helpactor;

import akka.actor.AbstractActor;
import com.example.message.AvgDataMessageDelivery;
import com.example.message.ConfirmMessage;
import com.example.message.SensorDataMessageDelivery;
import com.example.message.StdDataMessageDelivery;

public class MyDestination extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorDataMessageDelivery.class, msg -> {
                    getSender().tell(new ConfirmMessage(msg.deliveryId, msg.s), getSelf());
                })
                .match(AvgDataMessageDelivery.class, msg -> {
                    getSender().tell(new ConfirmMessage(msg.deliveryId, msg.s), getSelf());
                })
                .match(StdDataMessageDelivery.class, msg -> {
                    getSender().tell(new ConfirmMessage(msg.deliveryId, msg.s), getSelf());
                })
                .build();
    }

}
