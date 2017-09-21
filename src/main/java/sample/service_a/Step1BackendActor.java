package sample.service_a;

import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * STEP 1:
 * Too many individual calls leads to a violation of the SLA with service B.
 *
 * Solution: implement batching of X calls before calling the service.
 */
class Step1BackendActor extends BackendBaseActor {
    private static final String TOKEN = "-";
    private Map<ActorRef, Long> actorRefLongMap = new HashMap<>();

    private final int batchSize =
            this.getContext().getSystem().settings().config().getInt("service-a.batch-size");

    public Receive createReceive() {
        return receiveBuilder().match(Long.class, (Long id) -> addIdentifier(getSender(), id)).build();
    }

    private void addIdentifier(ActorRef sender, Long id) throws ExecutionException, InterruptedException {
        actorRefLongMap.put(sender, id);
        System.out.println("> actorRefLongMap size : " + actorRefLongMap.size());
        if (actorRefLongMap.size() >= batchSize) {
            CompletableFuture<String> serviceResponse = callService(generateIdsString());
            serviceResponse.thenAccept(s -> {
                for (ActorRef actor: actorRefLongMap.keySet()) {
                    actor.tell(s, getSelf());
                }
                actorRefLongMap.clear();
            }).get();
        }
    }

    private String generateIdsString() {
        StringBuilder sb = new StringBuilder(batchSize * 2);
        for(Long l : actorRefLongMap.values()) {
            sb.append(l);
            sb.append(TOKEN);
        }
        String ids =  sb.toString();
        return ids.substring(0, ids.length() - 1);
    }

    static Props props() {
        return Props.create(Step1BackendActor.class);
    }
}
