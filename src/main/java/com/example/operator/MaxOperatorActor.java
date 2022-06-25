package com.example.operator;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.persistence.*;
import com.example.MainPipeline;
import com.example.message.*;
import com.example.persistence.QueueDoubleState;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Queue;

public class MaxOperatorActor extends AbstractPersistentActorWithAtLeastOnceDelivery {

    private QueueDoubleState state = new QueueDoubleState();
    final private int windowSize;
    final private int windowSlide;
    String status;
    private final String persistenceId;
    private final ActorSelection destination;


    public MaxOperatorActor(int windowSize, int windowSlide, String persistenceId) {
        this.windowSlide = windowSlide;
        this.windowSize = windowSize;
        this.persistenceId = persistenceId;
        this.destination = getContext().actorSelection("/user/receiver-" + persistenceId.split("-")[1]);
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(SnapshotOffer.class, this::recoverSnap)
                .match(StdMessage.class, this::sendRecover)
                .match(RecoveryCompleted.class, this::recoverComplete)
                .match(ConfirmMessage.class, this::confirmRecover)
                .build();
    }
    private void sendRecover(StdMessage msg) {
        System.out.println("recover send : " + msg + " " + msg.getKey() + " " + msg.getValue());
        deliver(destination, deliveryId -> new StdDataMessageDelivery(deliveryId, msg));
        getContext().getSelf().tell(msg, null);
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
                .match(StdMessage.class, this::maxPayload)
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
    private void maxPayload(StdMessage message) {
        state.update(message);

        persist(message, (StdMessage m) -> {
            deliver(destination, deliveryId -> new StdDataMessageDelivery(deliveryId, m));
        });

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

    public static Props props(int windowSize, int windowSlide, String persistenceId) {
        return Props.create(MaxOperatorActor.class, windowSize, windowSlide, persistenceId);
    }

    @Override
    public String persistenceId() {
        return this.persistenceId;
    }

}
