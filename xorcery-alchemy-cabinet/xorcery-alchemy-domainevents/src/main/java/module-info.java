module xorcery.alchemy.domainevents {
    exports dev.xorcery.alchemy.domainevents;

    requires xorcery.alchemy.jar;

    requires xorcery.domainevents.api;
    requires xorcery.reactivestreams.api;

    requires com.graphqljava;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
}