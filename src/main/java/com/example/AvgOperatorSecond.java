package com.example;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

public class AvgOperatorSecond extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
        return null;
    }

    public static Props props() {
        return Props.create(AvgOperatorSecond.class);
    }
}
