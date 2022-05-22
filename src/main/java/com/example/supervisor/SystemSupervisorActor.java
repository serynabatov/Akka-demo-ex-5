package com.example.supervisor;

import akka.actor.AbstractActor;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import com.example.message.ExceptionMessage;

import java.time.Duration;

public class SystemSupervisorActor extends AbstractActor {

    private static SupervisorStrategy strategy = new AllForOneStrategy(
          10,
            Duration.ofSeconds(10),
            DeciderBuilder.match(
                    Exception.class,
                    e -> SupervisorStrategy.restart()
            ).match(
                    ExceptionMessage.class,
                    e -> (SupervisorStrategy.Directive) SupervisorStrategy.restart()
            ).build()
    );

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(
                Props.class,
                props -> {
                    getSender().tell(getContext().actorOf(props), getSelf());
                }
        ).build();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public static Props props() {
        return Props.create(SystemSupervisorActor.class);
    }
}
