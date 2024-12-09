module xorcery.alchemy.domainevents {
    requires analytics.api;
    requires xorcery.alchemy.jar;
    requires xorcery.domainevents.api;
    requires xorcery.reactivestreams.api;
    requires com.graphqljava;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires xorcery.neo4j.shaded;
    exports dev.xorcery.alchemy.domainevents;
}