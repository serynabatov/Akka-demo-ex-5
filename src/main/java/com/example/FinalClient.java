package com.example;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

public class FinalClient extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AvgMessage.class, this::displayMessage)
                .build();
    }

    private void displayMessage(AvgMessage message) {
        log().info(message.getKey() + " " + message.getValue());
    }

    static Props props() {
        return Props.create(FinalClient.class);
    }

}
