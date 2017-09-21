package sample.service_a;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Promise;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.longSegment;
import static akka.http.javadsl.server.PathMatchers.segment;

public class StreamMain extends AllDirectives {

    private static Flow<Long, String, NotUsed> commonFlow = null;

    private static RunnableGraph<Sink<Pair, NotUsed>> someSource = null;

    public static void main(String[] args) throws IOException {
        final ActorSystem system = ActorSystem.create("ServiceA");
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final StreamMain app = new StreamMain();
        final ActorRef streamActor = system.actorOf(StreamActor.props());
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute(streamActor).flow(system, materializer);
        final CompletionStage<ServerBinding> binding =
                http.bindAndHandle(
                        routeFlow,
                        ConnectHttp.toHost("localhost", 8080),
                        materializer);

        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return
        System.in.read(); // let it run until user presses return
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done

    }

    private Route createRoute(ActorRef streamActor) {
        return route(
                path(segment("services").slash(longSegment()), id ->
                        get(() -> {
                            Promise<HttpResponse> promise = Futures.promise();
                            streamActor.tell(new Pair(id, promise), ActorRef.noSender());
                            return completeWithFuture(FutureConverters.toJava(promise.future()));
                        })));
    }
}