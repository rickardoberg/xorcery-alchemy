module xorcery.alchemy.script {
    exports dev.xorcery.alchemy.script;
    exports dev.xorcery.alchemy.script.source;
    exports dev.xorcery.alchemy.script.transmute;

    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.api;

    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.openjdk.nashorn;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
}