package sample.service_a;

import akka.NotUsed;
import akka.actor.*;

import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.util.concurrent.CompletionStage;

import static akka.pattern.PatternsCS.ask;

import static akka.http.javadsl.server.PathMatchers.*;

class ActorMain extends AllDirectives {

    private static final Long TIMEOUT = 5000L;

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("ServiceA");
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final ActorMain app = new ActorMain();
        ActorRef serviceBackendActor = null;

        switch (Integer.parseInt(args[0])) {
            case 0:
                serviceBackendActor = system.actorOf(Step0BackendActor.props(), "step0");
                break;
            case 1:
                serviceBackendActor = system.actorOf(Step1BackendActor.props(), "step1");
                break;
            case 2:
                serviceBackendActor = system.actorOf(Step2BackendActor.props(), "step2");
                break;
            case 3:
                serviceBackendActor = system.actorOf(Step3BackendActor.props(), "step3");
                break;
            default:
                throw new RuntimeException("Unknown example.");
        }

        //final ActorRef serviceBackendActor = system.actorOf(BackendActor.props(), "backendActor");
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute(serviceBackendActor).flow(system, materializer);

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

    private Route createRoute(ActorRef serviceBackendActor) {
        return route(
                path(segment("services").slash(longSegment()), id ->
                        get(() -> {
                            CompletionStage<HttpResponse> response = ask(serviceBackendActor, id, TIMEOUT).thenApply(o -> {
                                if (o instanceof String) {
                                    return HttpResponse.create().withEntity((String) o);
                                } else {
                                    return HttpResponse.create().withStatus(StatusCodes.BAD_REQUEST);
                                }
                            });

                            return completeWithFuture(response);
                        })));
    }
}
