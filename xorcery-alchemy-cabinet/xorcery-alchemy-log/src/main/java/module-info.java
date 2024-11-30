module xorcery.alchemy.output.log {
    exports dev.xorcery.alchemy.result.log.transmute;
    exports dev.xorcery.alchemy.result.log.result;

    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
}