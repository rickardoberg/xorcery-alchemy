module xorcery.alchemy.jar {
    exports dev.xorcery.alchemy.jar;

    requires xorcery.reactivestreams.api;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires java.compiler;
}