module xorcery.alchemy.opensearch {
    exports dev.xorcery.alchemy.opensearch.result;

    requires xorcery.opensearch.client;
    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.api;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires jakarta.ws.rs;
}