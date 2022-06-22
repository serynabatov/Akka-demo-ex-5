package com.example.operator;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.persistence.*;
import com.example.MainPipeline;
import com.example.message.*;
import com.example.persistence.QueueState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;


public class AvgOperatorActor extends AbstractPersistentActorWithAtLeastOnceDelivery {

    private QueueState state = new QueueState();
    final private int windowSize;
    final private int windowSlide;
    String status;

    private final ActorSelection destination = getContext().actorSelection("/user/receiver");

    private final String persistenceId;

    public static Vector<ActorRef> nextStep;

    public AvgOperatorActor(int windowSize, int windowSlide, String persistenceId) {
        this.windowSize = windowSize;
        this.windowSlide = windowSlide;
        this.persistenceId = persistenceId;
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(SnapshotOffer.class, this::recoverSnap)
                .match(SensorDataMessage.class, this::sendRecover)
                .match(ConfirmMessage.class, this::confirmRecover)
                .match(RecoveryCompleted.class, this::recoverComplete)
                .build();
    }


    private void sendRecover(SensorDataMessage msg) {
        System.out.println("recover send : "+msg.getValue());
        deliver(destination, deliveryId -> new SensorDataMessageDelivery(deliveryId, msg));
    }
    public void confirmRecover(ConfirmMessage msg) {
        System.out.println("confirm recover: " + msg);
        confirmDelivery(msg.deliveryId);
    }
    public void recoverSnap(SnapshotOffer snapshotOffer) {
        @SuppressWarnings("unchecked")
        Pair<String, AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot> snapshot =
                (Pair<String, AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot>) snapshotOffer.snapshot();
        status = snapshot.getLeft();
        System.out.println("recover status by snapshot : "+status);
        setDeliverySnapshot(snapshot.getRight());
    }
    public void recoverComplete(RecoveryCompleted recoveryCompleted) {
        System.out.println("recover complete");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorDataMessage.class, this::averagePayload)
                .match(ConfirmMessage.class, this::confirmDelivery)
                .match(SaveSnapshotSuccess.class, this::saveSnapSuc)
                .match(AtLeastOnceDelivery.UnconfirmedWarning.class, this::unconfirm)
                .build();
    }

    public void saveSnapSuc(SaveSnapshotSuccess saveSnapshotSuccess) {
        System.out.println("save snap suc : "+saveSnapshotSuccess);
    }
    public void unconfirm(AtLeastOnceDelivery.UnconfirmedWarning unconfirmedWarning) {
        unconfirmedWarning.getUnconfirmedDeliveries().stream().forEach(ud->{
            System.out.println("unconfirm : "+ud.message()+" : "+ud.deliveryId());
        });
    }

    private void confirmDelivery(ConfirmMessage m) {
        persist(m, (ConfirmMessage e) -> {
            confirmDelivery(e.deliveryId);
        } );
    }


    private void averagePayload(SensorDataMessage message) {
        state.update(message);

        persist(message, (SensorDataMessage m) -> {
            deliver(destination, deliveryId -> new SensorDataMessageDelivery(deliveryId, m));
        });

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
                    System.out.println("This is NoSuchElementException, you should be careful in how you check the data");
                }
            }

            state.replace(message, values);
            nextStep.get(message.getKey().hashCode() % MainPipeline.REPLICAS).tell(new AvgMessage(message.getKey(), (double) sum / windowSize), this.getSelf());
        }
    }

    public static Props props(int windowSize, int windowSlide, String persistenceId) {
        return Props.create(AvgOperatorActor.class, () -> new AvgOperatorActor(windowSize, windowSlide, persistenceId));
    }

    @Override
    public String persistenceId() {
        return this.persistenceId;
    }
}
