package sample.service_a;

import akka.actor.AbstractActor;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.ActorMaterializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public abstract class BackendBaseActor extends AbstractActor {
    private final ActorMaterializer materializer = ActorMaterializer.create(this.getContext().getSystem());
    private final String URI = getContext().getSystem().settings().config().getString("service-b-uri");

    protected CompletableFuture<String> callService(String ids) throws ExecutionException, InterruptedException {
        return Http.get(this.getContext().getSystem())
                .singleRequest(HttpRequest.create()
                        .withUri(URI + ids)
                        .withMethod(HttpMethods.GET), materializer)
                .thenCompose(response -> Unmarshaller.entityToString().unmarshal(response.entity(), this.getContext().dispatcher(), materializer))
                .toCompletableFuture();
    }
}
