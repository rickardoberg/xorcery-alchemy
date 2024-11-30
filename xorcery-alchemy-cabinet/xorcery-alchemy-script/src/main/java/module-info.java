module xorcery.alchemy.script {
    exports dev.xorcery.alchemy.script;
    exports dev.xorcery.alchemy.script.result;
    exports dev.xorcery.alchemy.script.source;
    exports dev.xorcery.alchemy.script.transmute;

    requires xorcery.alchemy.jar;
    requires org.apache.logging.log4j;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.openjdk.nashorn;
}