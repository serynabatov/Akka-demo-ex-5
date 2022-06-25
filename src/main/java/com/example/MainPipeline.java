package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.example.helpactor.MyDestination;
import com.example.message.SimpleMissage;
import com.example.operator.AvgOperatorActor;
import com.example.operator.MaxOperatorActor;
import com.example.operator.StdDevOperatorActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class MainPipeline {

    private final List<SimpleMissage> sensors;
    Vector<ActorRef> avgStep = new Vector<>();
    Vector<ActorRef> maxStep = new Vector<>();
    Vector<ActorRef> stdDevStep = new Vector<>();

    public static final int REPLICAS = 4;
    Duration timeout = Duration.create(5, TimeUnit.SECONDS);


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

    public void startDefault() {

        File journalFolder = new File("akka/persistence/journal"); journalFolder.mkdirs();
        File snapshotFolder = new File("akka/persistence/snapshots"); snapshotFolder.mkdirs();
        try {
            FileUtils.deleteDirectory(journalFolder);
            FileUtils.deleteDirectory(snapshotFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> configs = ImmutableMap.<String, Object>builder()
                .put("akka.persistence.journal.plugin", "akka.persistence.journal.leveldb")
                .put("akka.persistence.snapshot-store.plugin", "akka.persistence.snapshot-store.local")
                .put("akka.persistence.journal.leveldb.dir", "akka/persistence/journal")
                .put("akka.persistence.snapshot-store.local.dir", "akka/persistence/snapshots")
                .put("akka.actor.warn-about-java-serializer-usage", "false")
                .put("akka.persistence.at-least-once-delivery.redeliver-interval", "1s")
                .put("akka.persistence.at-least-once-delivery.warn-after-number-of-unconfirmed-attempts", 3)
                .build();
        Config config = ConfigFactory.parseMap(configs);

        ActorSystem sys = ActorSystem.create("ActorSystem", config);
        Vector<ActorRef> destinations = new Vector<>();

        for (int i = 0; i < REPLICAS; i++){
            try {
                avgStep.add(sys.actorOf(AvgOperatorActor.props(windowSizeAvg, windowSlideAvg, "add-"+i), "add-"+i));
                stdDevStep.add(sys.actorOf(StdDevOperatorActor.props(windowSizeStd, windowSlideStd, "std-" + i),"std-" + i));
                maxStep.add(sys.actorOf(MaxOperatorActor.props(windowSizeMax, windowSlideMax, "max-" + i), "max-" + i));
                destinations.add(sys.actorOf(Props.create(MyDestination.class), "receiver-" + i));
                try {
                    Thread.sleep(500);
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AvgOperatorActor.nextStep = stdDevStep;
        StdDevOperatorActor.nextStep = maxStep;

        for (SimpleMissage message : sensors) {
            try {
                Future<Object> future = ask(avgStep.get(message.getKey().hashCode() % REPLICAS),
                        message, 1000);

                future.result(timeout, null);
            } catch (Exception ignored) {
            }
        }

        avgStep.clear();
        maxStep.clear();
        stdDevStep.clear();
        destinations.clear();

        sys.terminate();
    }

    public void fail(int theFailActorNumber) {

        File journalFolder = new File("akka/persistence/journal"); journalFolder.mkdirs();
        File snapshotFolder = new File("akka/persistence/snapshots"); snapshotFolder.mkdirs();
        try {
            FileUtils.deleteDirectory(journalFolder);
            FileUtils.deleteDirectory(snapshotFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> configs = ImmutableMap.<String, Object>builder()
                .put("akka.persistence.journal.plugin", "akka.persistence.journal.leveldb")
                .put("akka.persistence.snapshot-store.plugin", "akka.persistence.snapshot-store.local")
                .put("akka.persistence.journal.leveldb.dir", "akka/persistence/journal")
                .put("akka.persistence.snapshot-store.local.dir", "akka/persistence/snapshots")
                .put("akka.actor.warn-about-java-serializer-usage", "false")
                .put("akka.persistence.at-least-once-delivery.redeliver-interval", "1s")
                .put("akka.persistence.at-least-once-delivery.warn-after-number-of-unconfirmed-attempts", 3)
                /*.put("akka.actor.provider", "remote")
                .put("akka.remote.enabled-transports", List.of("akka.remote.netty.tcp"))
                .put("akka.remote.netty.tcp.hostname", "127.0.0.1")
                .put("akka.remote.netty.tcp.port", 2552)*/
                .build();
        Config config = ConfigFactory.parseMap(configs);

        ActorSystem sys = ActorSystem.create("ActorSystem", config);
        Vector<ActorRef> destinations = new Vector<>();
        for (int i = 0; i < REPLICAS; i++){
            try {
                avgStep.add(sys.actorOf(AvgOperatorActor.props(windowSizeAvg, windowSlideAvg, "add-"+i), "add-"+i));
                stdDevStep.add(sys.actorOf(StdDevOperatorActor.props(windowSizeStd, windowSlideStd, "std-" + i),"std-" + i));
                maxStep.add(sys.actorOf(MaxOperatorActor.props(windowSizeMax, windowSlideMax, "max-" + i), "max-" + i));
                destinations.add(sys.actorOf(Props.create(MyDestination.class), "receiver-" + i));
                try {
                    Thread.sleep(500);
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AvgOperatorActor.nextStep = stdDevStep;
        StdDevOperatorActor.nextStep = maxStep;

        int k = 0;

        for (SimpleMissage message : sensors) {
            if (k == theFailActorNumber) {
                break;
            }
            try {
                Future<Object> future = ask(avgStep.get(message.getKey().hashCode() % REPLICAS),
                        message, 1000);

                future.result(timeout, null);
            } catch (Exception ignored) {
            }
            k++;
        }

        sys.terminate();
        System.out.println("SYSTEM HAS BEEN STOPPED");

        avgStep.clear();
        stdDevStep.clear();
        maxStep.clear();
        destinations.clear();

//        Map<String, Object> configs2 = ImmutableMap.<String, Object>builder()
//                .put("akka.persistence.journal.plugin", "akka.persistence.journal.leveldb")
//                .put("akka.persistence.snapshot-store.plugin", "akka.persistence.snapshot-store.local")
//                .put("akka.persistence.journal.leveldb.dir", "akka/persistence/journal")
//                .put("akka.persistence.snapshot-store.local.dir", "akka/persistence/snapshots")
//                .put("akka.actor.warn-about-java-serializer-usage", "false")
//                .put("akka.persistence.at-least-once-delivery.redeliver-interval", "1s")
//                .put("akka.persistence.at-least-once-delivery.warn-after-number-of-unconfirmed-attempts", 3)
//                .put("akka.actor.provider", "remote")
//                .put("akka.remote.enabled-transports", List.of("akka.remote.netty.tcp"))
//                .put("akka.remote.netty.tcp.hostname", "127.0.0.1")
//                .put("akka.remote.netty.tcp.port", 2652)
//                .build();

        config = ConfigFactory.parseMap(configs);

        sys = ActorSystem.create("ActorSystem", config);
        for (int i = 0; i < REPLICAS; i++){
            try {
                avgStep.add(sys.actorOf(AvgOperatorActor.props(windowSizeAvg, windowSlideAvg, "add-"+i), "add-"+i));
                stdDevStep.add(sys.actorOf(StdDevOperatorActor.props(windowSizeStd, windowSlideStd, "std-" + i),"std-" + i));
                maxStep.add(sys.actorOf(MaxOperatorActor.props(windowSizeMax, windowSlideMax, "max-" + i), "max-" + i));
                destinations.add(sys.actorOf(Props.create(MyDestination.class), "receiver-" + i));
                try {
                    Thread.sleep(500);
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
            }
        }

        AvgOperatorActor.nextStep = stdDevStep;
        StdDevOperatorActor.nextStep = maxStep;

        for (int i = k; i < sensors.size(); i++) {
            try {
                Future<Object> future = ask(avgStep.get(sensors.get(i).getKey().hashCode() % REPLICAS),
                        sensors.get(i), 1000);
                future.result(timeout, null);
            } catch (Exception ignored) {
            }
        }

        sys.terminate();
        avgStep.clear();
        stdDevStep.clear();
        maxStep.clear();
        destinations.clear();
    }

}
