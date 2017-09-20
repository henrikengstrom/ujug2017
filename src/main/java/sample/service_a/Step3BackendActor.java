package sample.service_a;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * STEP3:
 * Prevent flooding of service B (in the case of a massive amount of simultaneous calls).
 *
 * Solution:
 * Limit the number of simultaneous calls to service B.
 * Also change the underlying map implementation as we no longer can just remove all calls when there are multiple calls to the backend.
 */
class Step3BackendActor extends BackendBaseActor {
    private static final String TOKEN = "-";
    private Map<ActorRef, Long> incomingRequests = new ConcurrentHashMap<>();
    private Set<ActorRef> inFlightRequests = new HashSet<>();

    private boolean requestDone = false;
    private Cancellable activeScheduler = null;

    private final int batchSize =
            this.getContext().getSystem().settings().config().getInt("service-a.batch-size");

    private final FiniteDuration batchFrequencyInterval =
            Duration.create(
                    context().system().settings().config().getDuration(
                            "service-a.batch-frequency", MILLISECONDS), MILLISECONDS);

    private final int concurrentCallsLimit =
            this.getContext().getSystem().settings().config().getInt("service-a.concurrent-calls");

    private int concurrentCalls = 0;


    @Override
    public void preStart() throws Exception {
        activeScheduler =
                this.getContext().getSystem().scheduler().schedule(
                        batchFrequencyInterval,
                        batchFrequencyInterval,
                        getSelf(),
                        Send.INSTANCE,
                        getContext().dispatcher(),
                        getSelf());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        activeScheduler.cancel();
    }

    public Step3BackendActor() {
    }

    public Receive createReceive() {
        return receiveBuilder()
                .match(Long.class, (Long id) -> addIdentifier(getSender(), id))
                .match(Send.class, (Send i) -> {
                    if (requestDone) {
                        requestDone = false;
                    } else {
                        sendData();
                    }

                })
                .build();
    }

    private void sendData() throws ExecutionException, InterruptedException {
        if (concurrentCalls >= concurrentCallsLimit) {
            System.out.println(">>>>>>>>>>>>> MAX LIMIT REACHED... WAITING FOR A BIT!");
        }

        if (incomingRequests.size() > 0 && concurrentCalls < concurrentCallsLimit) {
            requestDone = true;
            concurrentCalls++;
            System.out.println(">>>>>>>>>>> + CONCURRENT CALLS: " + concurrentCalls);

            // Get this batch from the incoming requests
            Set<ActorRef> batched = incomingRequests.keySet().stream().limit(batchSize).collect(Collectors.toSet());

            // Get the ids from the incoming requests
            String ids = generateIdsString(batched);

            // Remove the batch actors from the incoming request maps
            for (ActorRef actorRef : batched) {
                incomingRequests.remove(actorRef);
            }

            CompletableFuture<String> serviceResponse = callService(ids);
            serviceResponse.thenAccept(s -> {
                batched.stream().forEach(actorRef -> {
                    actorRef.tell(s, getSelf());
                    inFlightRequests.remove(actorRef);
                });
                concurrentCalls--;
                System.out.println(">>>>>>>>>>> - CONCURRENT CALLS: " + concurrentCalls);
            });
        }
    }

    private void addIdentifier(ActorRef sender, Long id) throws ExecutionException, InterruptedException {
        incomingRequests.put(sender, id);
        //System.out.println(">>> incomingRequests size : " + incomingRequests.size());
        if (incomingRequests.size() >= batchSize) {
            sendData();
        }
    }

    private String generateIdsString(Set<ActorRef> batched) {
        StringBuilder sb = new StringBuilder(batchSize * 2);
        for (ActorRef actorRef : batched) {
            sb.append(incomingRequests.get(actorRef));
            sb.append(TOKEN);
        }
        String ids =  sb.toString();
        System.out.println(">>> IDS : " + ids);
        return ids.substring(0, ids.length() - 1);
    }

    static Props props() {
        return Props.create(Step3BackendActor.class, () -> new Step3BackendActor());
    }

    static class Send {
        public static final Send INSTANCE = new Send();
        private Send() {
        }
    }
}
