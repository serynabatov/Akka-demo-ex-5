package com.example.operator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.AbstractPersistentActorWithAtLeastOnceDelivery;
import akka.persistence.SnapshotOffer;
import com.example.MainPipeline;
import com.example.exception.FaultException;
import com.example.message.ExceptionMessage;
import com.example.message.SensorDataMessage;
import com.example.message.AvgMessage;
import com.example.persistence.QueueState;

import java.util.*;


public class AvgOperatorActor extends AbstractPersistentActorWithAtLeastOnceDelivery {

    private QueueState state = new QueueState();
    final private int windowSize;
    final private int windowSlide;
    private final String persistenceId;
    private final int snapShotInterval = 1000;

    public static Vector<ActorRef> nextStep;

    public AvgOperatorActor(int windowSize, int windowSlide, String persistenceId) {
        this.windowSize = windowSize;
        this.windowSlide = windowSlide;
        this.persistenceId = persistenceId;
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(SensorDataMessage.class, state::update)
                .match(SnapshotOffer.class, ss -> state = (QueueState) ss.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorDataMessage.class, this::averagePayload)
                .match(ExceptionMessage.class, this::exception)
                .build();
    }

    private void averagePayload(SensorDataMessage message) {
        System.out.println(message);
        state.update(message);

        persist(message, (SensorDataMessage m) -> {
            getContext().getSystem().getEventStream().publish(m);
            if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0) {
                saveSnapshot(state.copy());
            }
        });

        //state.update(message);
        if (state.getSizeByKey(message) == windowSize) {
            Queue<Integer> values = state.get(message.getKey());
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

            state.replace(message, values);
            nextStep.get(message.getKey().hashCode() % MainPipeline.REPLICAS).tell(new AvgMessage(message.getKey(), (double) sum / windowSize), this.getSelf());
        }
    }

    private void exception(ExceptionMessage message) throws FaultException {
        System.out.println("Here we are emulating an error! " + message.getKey());
        throw new FaultException();
    }

    public static Props props(int windowSize, int windowSlide, String persistenceId) {
        return Props.create(AvgOperatorActor.class, windowSize, windowSlide, persistenceId);
    }

    @Override
    public String persistenceId() {
        return this.persistenceId;
    }
}
