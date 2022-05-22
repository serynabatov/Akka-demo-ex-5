package com.example.operator;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.example.SystemProcessingMain;
import com.example.message.AvgMessage;
import com.example.message.MaxMessage;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.Vector;

public class MaxOperatorActor extends AbstractLoggingActor {


    private HashMap<String, Queue<Double>> storedValues = new HashMap<>(); // store values here
    final private int windowSize = 2;
    final private int windowSlide = 1;
    public static Vector<ActorRef> nextStep;


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AvgMessage.class, this::maxPayload)
                .build();
    }

    private void maxPayload(AvgMessage message) {
        storedValues.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
        if (storedValues.get(message.getKey()).size() == windowSize) {
            Queue<Double> values = storedValues.get(message.getKey());

            Double maxValue = values.stream().max(Double::compare).get();

            for (int i = 0; i < windowSlide; i++) {
                try {
                    values.remove();
                } catch (Exception e) {
                    log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            storedValues.replace(message.getKey(), values);
            nextStep.get(message.getKey().hashCode() % SystemProcessingMain.REPLICAS).tell(new MaxMessage(message.getKey(), maxValue), this.getSelf());
        }
    }

    public static Props props() {
        return Props.create(MaxOperatorActor.class);
    }
}
