package sample.service_a;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * STEP2:
 * If the number of requests fluctuate, i.e. if it does not match our batch size, it will cause some requests to time out.
 *
 * Solution: create a scheduled call to service B to handle any lingering requests in the internal map.
 * Also, prevent that calls happen more frequently than 1s (i.e. if requests have just been requestDone it should wait at least the batch frequency time before sending again).
 */
class Step2BackendActor extends BackendBaseActor {
    private static final String TOKEN = "-";
    private Map<ActorRef, Long> actorRefLongMap = new HashMap<>();
    private boolean requestDone = false;
    private Cancellable activeScheduler = null;

    private final int batchSize =
            this.getContext().getSystem().settings().config().getInt("service-a.batch-size");

    private final FiniteDuration batchFrequencyInterval =
            Duration.create(
                    context().system().settings().config().getDuration(
                            "service-a.batch-frequency", MILLISECONDS), MILLISECONDS);

    @Override
    public void preStart() throws Exception {
        activeScheduler =
                this.getContext().getSystem().scheduler().schedule(
                        batchFrequencyInterval,
                        batchFrequencyInterval,
                        getSelf(),
                        CheckSendStatus.INSTANCE,
                        getContext().dispatcher(),
                        getSelf());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        activeScheduler.cancel();
    }

    public Step2BackendActor() {
    }

    public Receive createReceive() {
        return receiveBuilder()
                .match(Long.class, (Long id) -> addIdentifier(getSender(), id))
                .match(CheckSendStatus.class, (CheckSendStatus i) -> {
                    if (requestDone) {
                        requestDone = false;
                    } else {
                        sendData();
                    }

                })
                .build();
    }

    private void sendData() throws ExecutionException, InterruptedException {
        if (actorRefLongMap.size() > 0) {
            requestDone = true;

            CompletableFuture<String> serviceResponse = callService(generateIdsString());
            serviceResponse.thenAccept(s -> {
                for (ActorRef actor: actorRefLongMap.keySet()) {
                    actor.tell(s, getSelf());
                }
                actorRefLongMap.clear();
            }).get();
        }
    }

    private void addIdentifier(ActorRef sender, Long id) throws ExecutionException, InterruptedException {
        actorRefLongMap.put(sender, id);
        System.out.println(">>> actorRefLongMap size : " + actorRefLongMap.size());
        if (actorRefLongMap.size() >= batchSize) {
            sendData();
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
        return Props.create(Step2BackendActor.class, () -> new Step2BackendActor());
    }

    static class CheckSendStatus {
        public static final CheckSendStatus INSTANCE = new CheckSendStatus();
        private CheckSendStatus() {
        }
    }
}
