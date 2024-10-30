module xorcery.alchemy.output.log {
    exports com.exoreaction.xorcery.alchemy.result.log;

    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
}