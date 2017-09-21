package sample.service_a;

import akka.NotUsed;
import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.MergeHub;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class StreamActor extends AbstractLoggingActor {
    final static String TOKEN = "-";

    final private Sink<Pair, NotUsed> sink;

    final ActorMaterializer materializer = ActorMaterializer.create(getContext().getSystem());

    final int batchSize =
            getContext().getSystem().settings().config().getInt("service-a.batch-size");
    final FiniteDuration batchFrequency =
            Duration.create(getContext().getSystem().settings().config().getDuration(
                    "service-a.batch-frequency", MILLISECONDS), MILLISECONDS);
    final int concurrentCallsLimit =
            getContext().getSystem().settings().config().getInt("service-a.concurrent-calls");
    String serviceURI = getContext().getSystem().settings().config().getString("service-b-uri");

    public StreamActor() {
        this.sink =
                MergeHub.of(Pair.class) // use MergeHub to merge all incoming messages into one "flow"
                        .groupedWithin(batchSize, batchFrequency)
                        .map(pairs -> generateIdsString(pairs))
                        .mapAsync(concurrentCallsLimit, generatedResult -> callService(generatedResult))
                        .to(Sink.ignore())
                        .run(materializer);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Pair.class, pair -> {
            Source.single(pair).runWith(sink, materializer);
        }).build();
    }

    private Pair<String, List<Pair>> generateIdsString(List<Pair> pairs) {
        StringBuilder sb = new StringBuilder(batchSize * 2);
        ListIterator<Pair> list = pairs.listIterator();
        while (list.hasNext()) {
            long l = (Long) list.next().first();
            sb.append(l);
            sb.append(TOKEN);
        }
        String result = sb.toString();
        System.out.println("IDS : " + result);
        return new Pair(result.substring(0, result.length() - 1), pairs);
    }

    CompletionStage<Object> callService(Pair<String, List<Pair>> input) {
        try {
            Http.get(getContext().getSystem())
                    .singleRequest(HttpRequest.create()
                            .withUri(serviceURI + input.first())
                            .withMethod(HttpMethods.GET), materializer)
                    .thenCompose(response -> {
                        try {
                            String result = Unmarshaller.entityToString().unmarshal(response.entity(), getContext().dispatcher(), materializer).toCompletableFuture().get();
                            for (Pair pair : input.second()) {
                                HttpResponse resp = HttpResponse.create().withStatus(StatusCodes.OK).withEntity(result);
                                // Complete the promise with the result from the backend
                                ((Promise) pair.second()).success(resp);
                            }
                        } catch (Exception e) {
                            log().error(e, "Oh oh!!");
                        }

                        // We don't really care about the result here as it's the completion of the Promise above we're after.
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return FutureConverters.toJava(Futures.successful(input));
    }

    static Props props() {
        return Props.create(StreamActor.class, () -> new StreamActor());
    }
}
