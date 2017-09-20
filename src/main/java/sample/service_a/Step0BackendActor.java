package sample.service_a;

import akka.actor.ActorRef;
import akka.actor.Props;
import static akka.pattern.PatternsCS.pipe;

import java.util.concurrent.CompletableFuture;


class Step0BackendActor extends BackendBaseActor {
    public Receive createReceive() {
        return receiveBuilder()
                .match(Long.class, (Long l) -> {
                    ActorRef caller = getSender();
                    CompletableFuture<String> futureResult = callService(String.valueOf(l));
                    pipe(futureResult, getContext().dispatcher()).to(caller);
                }).build();
    }

    static Props props() {
        return Props.create(Step0BackendActor.class, () -> new Step0BackendActor());
    }
}
