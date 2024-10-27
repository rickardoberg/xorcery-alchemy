open module xorcery.alchemy.runner {
    exports com.exoreaction.xorcery.alchemy.runner;

    requires xorcery.alchemy.plugin;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.api;
}