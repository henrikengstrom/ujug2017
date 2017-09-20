package example;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

public class RSExample1 {
    public static void main(String[] args) {
        RSExample1 example1 = new RSExample1();
        example1.run1();
        example1.run2();
    }

    private void run1() {
        final ActorSystem actorSystem = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(actorSystem);
        final Source<Integer, NotUsed> source = Source.range(0, 10);
        final Flow<Integer, String, NotUsed> flow = Flow.fromFunction((Integer i) -> i.toString());
        final Sink<String, CompletionStage<Done>> sink = Sink.foreach(s -> System.out.println("Number: " + s));
        final RunnableGraph runnable = source.via(flow).to(sink);
        runnable.run(materializer);
        actorSystem.terminate();
    }

    private void run2() {
        final ActorSystem actorSystem = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(actorSystem);
        Source.range(0, 10)
                .map(Object::toString)
                .runForeach(s -> System.out.println("Number: " + s), materializer);
        actorSystem.terminate();
    }
}
