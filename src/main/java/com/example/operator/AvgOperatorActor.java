package com.example.operator;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.persistence.AbstractPersistentActorWithAtLeastOnceDelivery;
import com.example.MainPipeline;
import com.example.exception.FaultException;
import com.example.message.ExceptionMessage;
import com.example.message.SensorDataMessage;
import com.example.message.AvgMessage;

import java.util.*;


public class AvgOperatorActor extends AbstractPersistentActorWithAtLeastOnceDelivery {

    private HashMap<String, Queue<Integer>> storedValues = new HashMap<>(); // store values here
    final private int windowSize;
    final private int windowSlide;

    public static Vector<ActorRef> nextStep;

        public AvgOperatorActor(int windowSize, int windowSlide) {
        this.windowSize = windowSize;
        this.windowSlide = windowSlide;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorDataMessage.class, this::averagePayload)
                .match(ExceptionMessage.class, this::exception)
                .build();
    }

    @Override
    public Receive createReceiveRecover() {
            return null;
    }

    private void averagePayload(SensorDataMessage message) {
        storedValues.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
        System.out.println(storedValues);
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
                    //log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            storedValues.replace(message.getKey(), values);
            System.out.println((double) sum / windowSize);
            nextStep.get(message.getKey().hashCode() % MainPipeline.REPLICAS).tell(new AvgMessage(message.getKey(), (double) sum / windowSize), this.getSelf());
        }
    }

    private void exception(ExceptionMessage message) throws FaultException {
        System.out.println("Here we are emulating an error! " + message.getKey());
        throw new FaultException();
    }

    public static Props props(int windowSize, int windowSlide) {
        return Props.create(AvgOperatorActor.class, windowSize, windowSlide);
    }

    @Override
    public String persistenceId() {
        return "sample-id-1";
    }
}
