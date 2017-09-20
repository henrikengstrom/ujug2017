package sample.service_b;

import akka.actor.*;

import java.util.Arrays;

class BackendActor extends AbstractActor {
    private final ActorRef databaseActor;

    public BackendActor() {
        this.databaseActor = this.context().actorOf(DatabaseActor.props(), "databaseActor");
    }

    public Receive createReceive() {
        return receiveBuilder()
                .match(Identifiers.class, ids -> {
                    databaseActor.forward(ids, this.getContext());
                }).build();
    }

    static Props props() {
        return Props.create(BackendActor.class, () -> new BackendActor());
    }

    static class Identifiers {
        Long[] ids;

        public Identifiers(Long[] ids) {
            this.ids = ids;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Identifiers that = (Identifiers) o;

            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(ids, that.ids);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(ids);
        }
    }
}
