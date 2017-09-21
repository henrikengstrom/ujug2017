package sample.service_b;

import akka.actor.AbstractActor;
import akka.actor.Props;

public class DatabaseActor extends AbstractActor {

    public DatabaseActor() {
    }

    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(BackendActor.Identifiers.class, identifiers -> {
                    long totalTime = 0l;
                    for (long id : identifiers.ids) {
                        long simulatedDatabaseCallTime = id;
                        Thread.sleep(simulatedDatabaseCallTime);
                        totalTime += simulatedDatabaseCallTime;
                    }
                    getSender().tell("DB CALL TIME = " + totalTime + "ms\n", getSender());
                }).build();
    }

    static Props props() {
        return Props.create(DatabaseActor.class, () -> new DatabaseActor());
    }
}
