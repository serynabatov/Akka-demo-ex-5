package com.example.operator;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import com.example.message.AvgMessage;
import com.example.message.MaxMessage;
import com.example.message.StdMessage;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

public class StdDevOperatorActor extends AbstractLoggingActor {
    private HashMap<String, Queue<Double>> storedValues = new HashMap<>(); // store values here
    final private int windowSize = 2;
    final private int windowSlide = 1;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(MaxMessage.class, this::maxPayload)
                .build();
    }

    private void maxPayload(MaxMessage message) {
        storedValues.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
        if (storedValues.get(message.getKey()).size() == windowSize) {
            Queue<Double> values = storedValues.get(message.getKey());

            double sum = 0;
            for (double value : values) {
                sum += value;
            }

            double mean = sum / windowSize;

            sum = 0;
            for (double value : values) {
                sum += Math.pow((value - mean), 2);
            }

            mean = sum / (windowSize - 1);
            double dev = Math.sqrt(mean);
            for (int i = 0; i < windowSlide; i++) {
                try {
                    values.remove();
                } catch (Exception e) {
                    log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            storedValues.replace(message.getKey(), values);
            System.out.println("Result: " + message.getKey() + " " + dev);
        }
    }

    public static Props props() {
        return Props.create(StdDevOperatorActor.class);
    }

}
