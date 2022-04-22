package com.example;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

import java.util.*;

public class AvgOperatorActor extends AbstractLoggingActor {

    private HashMap<String, Queue<Integer>> storedValues = new HashMap<>(); // store values here
    private int id = 1;


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorDataMessage.class, this::averagePayload)
                .build();
    }

    // TODO: I don't get the part with the partitions: If prof meant to distribute the data on sight between the
    // different instances of the same operator => we need to check the different patterns aside what he has given us
    // another approach is just to give the different instances to check the different instances
    private void averagePayload(SensorDataMessage message) {
        if (Constants.partitionMap.containsKey(message.getKey()) && Constants.partitionMap.get(message.getKey()) == id) {
            mainPayload(message);
        }
    }

    private void mainPayload(SensorDataMessage message) {
        storedValues.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
        if (storedValues.get(message.getKey()).size() == Constants.windowSize) {
            Queue<Integer> values = storedValues.get(message.getKey());
            int sum = 0;
            for (Integer value : values) {
                sum += value;
            }

            for (int i = 0; i < Constants.windowSlide; i++) {
                try {
                    values.remove();
                } catch (Exception e) {
                    log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            storedValues.replace(message.getKey(), values);

            sender().tell(new AvgMessage(message.getKey(), (double) sum / Constants.windowSize), self());
        }
    }


    public static Props props() {
        return Props.create(AvgOperatorActor.class);
    }

}
