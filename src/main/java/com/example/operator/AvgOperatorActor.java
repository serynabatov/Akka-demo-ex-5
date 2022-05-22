package com.example.operator;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.example.SystemProcessingMain;
import com.example.message.MaxMessage;
import com.example.message.SensorDataMessage;
import com.example.message.AvgMessage;

import java.util.*;

import static com.example.SystemProcessingMain.sys;

public class AvgOperatorActor extends AbstractLoggingActor {

    private HashMap<String, Queue<Integer>> storedValues = new HashMap<>(); // store values here
    final private int windowSize = 3;
    final private int windowSlide = 2;

    public static Vector<ActorRef> nextStep;


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorDataMessage.class, this::averagePayload)
                .build();
    }

    private void averagePayload(SensorDataMessage message) {
        storedValues.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
        if (storedValues.get(message.getKey()).size() == windowSize) {
            Queue<Integer> values = storedValues.get(message.getKey());
            int sum = 0;
            for (Integer value : values) {
                sum += value;
            }

            for (int i = 0; i < windowSlide; i++) {
                try {
                    values.remove();
                } catch (Exception e) {
                    log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            storedValues.replace(message.getKey(), values);
            nextStep.get(message.getKey().hashCode() % SystemProcessingMain.REPLICAS).tell(new AvgMessage("value", (double) sum / windowSize), this.getSelf());
        }
    }


    public static Props props() {
        return Props.create(AvgOperatorActor.class);
    }

}
