package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.example.message.SensorDataMessage;
import com.example.operator.AvgOperatorActor;
import com.example.operator.MaxOperatorActor;
import com.example.operator.StdDevOperatorActor;
import com.example.supervisor.SystemSupervisorActor;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.impl.FutureConvertersImpl;

import static akka.pattern.Patterns.ask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SystemProcessingMain {
    public final static ActorSystem sys = ActorSystem.create("ActorSystem");
    public static final int REPLICAS = 4;
    public final static ExecutorService exec = Executors.newFixedThreadPool(8);

    public static void main(String[] args) throws InterruptedException, IOException {

        Duration timeout = Duration.create(5, TimeUnit.SECONDS);

        final ActorRef supervisor = sys.actorOf(SystemSupervisorActor.props(), "supervisor");

        Vector<ActorRef> avgStep = new Vector<>();
        Vector<ActorRef> maxStep = new Vector<>();
        Vector<ActorRef> stdDevStep = new Vector<>();


        List<SensorDataMessage> messages = new ArrayList<>() {
            {
                add(new SensorDataMessage("temperature", 2));
                add(new SensorDataMessage("temperature", 3));
                add(new SensorDataMessage("temperature", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 5));
                add(new SensorDataMessage("humidity", 5));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("temperature", 5));
                add(new SensorDataMessage("temperature", 5));
                add(new SensorDataMessage("temperature", 3));
            }
        };

        try {
            for (int i = 0; i < REPLICAS; i++){
                Future<Object> waitingAvg = ask(supervisor, AvgOperatorActor.props(), 500);
                Future<Object> waitingMax = ask(supervisor, MaxOperatorActor.props(), 500);
                Future<Object> waitingStd = ask(supervisor, StdDevOperatorActor.props(), 500);

                avgStep.add((ActorRef) waitingAvg.result(timeout, null));
                maxStep.add((ActorRef) waitingMax.result(timeout, null));
                stdDevStep.add((ActorRef) waitingStd.result(timeout, null));
            }

            AvgOperatorActor.nextStep = maxStep;
            MaxOperatorActor.nextStep = stdDevStep;

            for (SensorDataMessage message : messages) {
                exec.submit(()->avgStep.get(message.getKey().hashCode() % REPLICAS).tell(message, ActorRef.noSender()));
            }

            exec.shutdown();
            sys.terminate();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }


    }

}
