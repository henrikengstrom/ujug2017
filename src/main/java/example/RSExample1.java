package example;

import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

import static akka.pattern.PatternsCS.ask;

public class RSExample1 {
    public static void main(String[] args) {
        RSExample1 example1 = new RSExample1();
        example1.run1();
        example1.run2();
        example1.run3();
    }

    private void run1() {
        final ActorSystem actorSystem = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(actorSystem);
        final Source<Integer, NotUsed> source = Source.range(0, 10);
        final Flow<Integer, String, NotUsed> flow = Flow.fromFunction((Integer i) -> i.toString());
        final Sink<String, CompletionStage<Done>> sink = Sink.foreach(s -> System.out.println("Example1 - Number: " + s));
        final RunnableGraph runnable = source.via(flow).to(sink);
        runnable.run(materializer);
        actorSystem.terminate();
    }

    private void run2() {
        final ActorSystem actorSystem = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(actorSystem);
        Source.range(0, 10)
                .map(Object::toString)
                .runForeach(s -> System.out.println("Example 2 - Number: " + s), materializer);
        actorSystem.terminate();
    }

    private void run3() {
        final ActorSystem actorSystem = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(actorSystem);
        final ActorRef multiplier = actorSystem.actorOf(MultiplierActor.props());
        Source.range(0, 10)
                .mapAsync(1, x -> ask(multiplier, x, 1000L))
                .map(e -> (Integer) e)
                .runWith(
                        Sink.foreach(i -> System.out.println("Example 3 - Number: " + i)),
                        materializer);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            actorSystem.terminate();
        }
    }

    public static class MultiplierActor extends AbstractActor {
        @Override
        public Receive createReceive() {
            return receiveBuilder().match(Integer.class, i -> {
                getSender().tell(i * 2, getSelf());
            }).build();
        }

        public static Props props() {
            return Props.create(MultiplierActor.class);
        }
    }

}
