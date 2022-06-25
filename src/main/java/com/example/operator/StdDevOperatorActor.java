package com.example.operator;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.persistence.*;
import com.example.MainPipeline;
import com.example.message.*;
import com.example.persistence.QueueDoubleState;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.text.DecimalFormat;
import java.util.Queue;
import java.util.Vector;

public class StdDevOperatorActor extends AbstractPersistentActorWithAtLeastOnceDelivery {
    private QueueDoubleState state = new QueueDoubleState();
    final private int windowSize;
    final private int windowSlide;
    final private String persistenceId;
    final private DecimalFormat df = new DecimalFormat("###.###");
    public static Vector<ActorRef> nextStep;

    private final ActorSelection destination;

    String status;

    public StdDevOperatorActor(int windowSize, int windowSlide, String persistenceId) {
        this.windowSize = windowSize;
        this.windowSlide = windowSlide;
        this.persistenceId = persistenceId;
        this.destination = getContext().actorSelection("/user/receiver-" + persistenceId.split("-")[1]);
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(SnapshotOffer.class, this::recoverSnap)
                .match(AvgMessage.class, this::sendRecover)
                .match(ConfirmMessage.class, this::confirmRecover)
                .match(RecoveryCompleted.class, this::recoverComplete)
                .build();
    }

    private void sendRecover(AvgMessage msg) {
        System.out.println("recover send : " + msg + " " + msg.getKey() + " " + msg.getValue());
        deliver(destination, deliveryId -> new AvgDataMessageDelivery(deliveryId, msg));
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
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(AvgMessage.class, this::maxPayload)
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
    private void maxPayload(AvgMessage message) {
        state.update(message);

        persist(message, (AvgMessage m) -> {
            deliver(destination, deliveryId -> new AvgDataMessageDelivery(deliveryId, m));
        });


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

    public static Props props(int windowSize, int windowSlide, String persistenceId) {
        return Props.create(StdDevOperatorActor.class, windowSize, windowSlide, persistenceId);
    }

    @Override
    public String persistenceId() {
        return this.persistenceId;
    }

}
