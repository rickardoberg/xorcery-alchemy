open module xorcery.alchemy.transmute.jslt {
    exports com.exoreaction.xorcery.alchemy.transmute.jslt;
    exports com.exoreaction.xorcery.alchemy.transmute.jslt.functions;

    uses com.schibsted.spt.data.jslt.Function;

    requires xorcery.alchemy.jar;
    requires xorcery.configuration.api;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.reactivestreams;
    requires reactor.core;
    requires jslt;
    requires org.apache.logging.log4j;

    provides com.schibsted.spt.data.jslt.Function with
            com.exoreaction.xorcery.alchemy.transmute.jslt.functions.PowerFunction;
}