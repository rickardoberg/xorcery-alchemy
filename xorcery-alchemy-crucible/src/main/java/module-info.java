open module xorcery.alchemy.crucible {
    exports com.exoreaction.xorcery.alchemy.crucible;

    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.api;
    requires xorcery.core;

    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.api;
}