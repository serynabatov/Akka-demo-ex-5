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

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.Vector;

public class StdDevOperatorActor extends AbstractActor {
    //private HashMap<String, Queue<Double>> storedValues = new HashMap<>(); // store values here
    private QueueDoubleState state = new QueueDoubleState();
    final private int windowSize;
    final private int windowSlide;
    final private DecimalFormat df = new DecimalFormat("###.###");
    public static Vector<ActorRef> nextStep;

    public StdDevOperatorActor(int windowSize, int windowSlide) {
        this.windowSize = windowSize;
        this.windowSlide = windowSlide;
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(AvgMessage.class, this::maxPayload)
                .match(ExceptionMessage.class, this::exception)
                .build();
    }

    private void maxPayload(AvgMessage message) {
        state.update(message);
        if (state.getSizeByKey(message) == windowSize) {
            Queue<Double> values = state.get(message.getKey());

            double sum = 0;
            for (double value : values) {
                sum += value;
            }

            double mean = sum / windowSize;

            sum = 0;
            for (double value : values) {
                sum += Math.pow((value - mean), 2);
            }

            mean = sum / windowSize;
            double dev = Math.sqrt(mean);
            for (int i = 0; i < windowSlide; i++) {
                try {
                    values.remove();
                } catch (Exception e) {
                    //log().debug("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            state.replace(message, values);
            nextStep.get(message.getKey().hashCode() % MainPipeline.REPLICAS).tell(new StdMessage("max-" + message.getKey(),
                    Double.parseDouble(df.format(dev))), this.getSelf());
        }
    }

    private void exception(ExceptionMessage message) throws FaultException {
        System.out.println("Here we are emulating an error! " + message.getKey());
        throw new FaultException();
    }


    public static Props props(int windowSize, int windowSlide) {
        return Props.create(StdDevOperatorActor.class, windowSize, windowSlide);
    }

}
