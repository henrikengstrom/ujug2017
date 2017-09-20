package sample.service_b;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.util.StringTokenizer;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.segment;

import static akka.pattern.PatternsCS.ask;

class Main extends AllDirectives {
    private static final Long TIMEOUT = 30000L;

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("routes");
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Main app = new Main();
        final ActorRef serviceBackendActor = system.actorOf(BackendActor.props(), "backendActor");
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow =  app.createRoute(serviceBackendActor).flow(system, materializer);

        final CompletionStage<ServerBinding> binding =
                http.bindAndHandle(
                        routeFlow,
                        ConnectHttp.toHost("localhost", 8081),
                        materializer);

        System.out.println("Server online at http://localhost:8081/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done

    }

    private Long[] splitIds(String ids) {
        StringTokenizer tokenizer = new StringTokenizer(ids, "-");
        Long[] longIds = new Long[tokenizer.countTokens()];
        int pos = 0;
        while(tokenizer.hasMoreElements()) {
            longIds[pos++] = Long.parseLong((String) tokenizer.nextElement());
        }

        return longIds;
    }

    private Route createRoute(ActorRef serviceBackendActor) {
        return route(
                path(segment("services").slash(PathMatchers.segment()), ids ->
                    get(() -> {
                        System.out.println("IDS RECEIVED: " + ids);
                        CompletionStage<HttpResponse> response = ask(serviceBackendActor, new BackendActor.Identifiers(splitIds(ids)), TIMEOUT).thenApply(o -> {
                            if (o instanceof String) {
                                System.out.println(">>>> response from db actor: " + o.toString());
                                return HttpResponse.create().withEntity(o.toString());
                            } else {
                                return HttpResponse.create().withStatus(StatusCodes.BAD_REQUEST);
                            }
                        });
                        return completeWithFuture(response);
                    })));
    }
}