package com.example.operator;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.example.MainPipeline;
import com.example.exception.FaultException;
import com.example.message.AvgMessage;
import com.example.message.ExceptionMessage;
import com.example.message.MaxMessage;
import com.example.message.StdMessage;
import com.example.persistence.QueueDoubleState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.Vector;

public class MaxOperatorActor extends AbstractActor {


    private QueueDoubleState state = new QueueDoubleState();
    final private int windowSize;
    final private int windowSlide;
    public static Vector<ActorRef> nextStep;

    public MaxOperatorActor(int windowSize, int windowSlide) {
        this.windowSlide = windowSlide;
        this.windowSize = windowSize;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StdMessage.class, this::maxPayload)
                .match(ExceptionMessage.class, this::exception)
                .build();
    }

    private void maxPayload(StdMessage message) {
        state.update(message);
        if (state.getSizeByKey(message) == windowSize) {
            Queue<Double> values = state.get(message.getKey()); //storedValues.get(message.getKey());

            Double maxValue = values.stream().max(Double::compare).get();

            for (int i = 0; i < windowSlide; i++) {
                try {
                    values.remove();
                } catch (Exception e) {
                    //log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            state.replace(message, values);
            System.out.println("Result: " + message.getKey() + " " + maxValue);
        }
    }

    private void exception(ExceptionMessage message) throws FaultException {
        System.out.println("Here we are emulating an error! " + message.getKey());
        throw new FaultException();
    }

    public static Props props(int windowSize, int windowSlide) {
        return Props.create(MaxOperatorActor.class, windowSize, windowSlide);
    }
}
