open module xorcery.alchemy.jslt {
    exports dev.xorcery.alchemy.jslt.transmute.functions;
    exports dev.xorcery.alchemy.jslt.transmute;

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
            dev.xorcery.alchemy.jslt.transmute.functions.PowerFunction;
}