package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.example.message.SensorDataMessage;
import com.example.message.SimpleMissage;
import com.example.operator.AvgOperatorActor;
import com.example.operator.MaxOperatorActor;
import com.example.operator.StdDevOperatorActor;
import com.example.supervisor.SystemSupervisorActor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static akka.pattern.Patterns.ask;

public class MainPipeline {

    private final List<SimpleMissage> sensors;
    Vector<ActorRef> avgStep = new Vector<>();
    Vector<ActorRef> maxStep = new Vector<>();
    Vector<ActorRef> stdDevStep = new Vector<>();
    private final ActorSystem sys = ActorSystem.create("ActorSystem");
    private final ExecutorService exec = Executors.newFixedThreadPool(8);

    public static final int REPLICAS = 4;
    Duration timeout = Duration.create(5, TimeUnit.SECONDS);

    final ActorRef supervisor = sys.actorOf(SystemSupervisorActor.props(), "supervisor");

    final private int windowSizeAvg;
    final private int windowSlideAvg;
    final private int windowSizeMax;
    final private int windowSlideMax;
    final private int windowSizeStd;
    final private int windowSlideStd;

    public MainPipeline(List<SimpleMissage> sensorDataMessageList, int windowSizeAvg, int windowSlideAvg,
                        int windowSizeMax, int windowSlideMax, int windowSizeStd, int windowSlideStd)  {
        this.sensors = sensorDataMessageList;
        this.windowSizeAvg = windowSizeAvg;
        this.windowSlideAvg = windowSlideAvg;
        this.windowSizeMax = windowSizeMax;
        this.windowSlideMax = windowSlideMax;
        this.windowSizeStd = windowSizeStd;
        this.windowSlideStd = windowSlideStd;
    }

    public void start() throws InterruptedException, TimeoutException {

        for (int i = 0; i < REPLICAS; i++){
            Future<Object> waitingAvg = ask(supervisor, AvgOperatorActor.props(windowSizeAvg, windowSlideAvg), 500);
            Future<Object> waitingMax = ask(supervisor, MaxOperatorActor.props(windowSizeMax, windowSlideMax), 500);
            Future<Object> waitingStd = ask(supervisor, StdDevOperatorActor.props(windowSizeStd, windowSlideStd), 500);

            avgStep.add((ActorRef) waitingAvg.result(timeout, null));
            maxStep.add((ActorRef) waitingMax.result(timeout, null));
            stdDevStep.add((ActorRef) waitingStd.result(timeout, null));
        }

        AvgOperatorActor.nextStep = stdDevStep;
        StdDevOperatorActor.nextStep = maxStep;

        for (SimpleMissage message : sensors) {
            avgStep.get(message.getKey().hashCode() % REPLICAS).tell(message, ActorRef.noSender());
        }

        exec.shutdown();
        sys.terminate();
    }

}
