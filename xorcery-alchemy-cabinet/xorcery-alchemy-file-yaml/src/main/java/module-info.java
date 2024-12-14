module xorcery.alchemy.file.yaml {
    exports dev.xorcery.alchemy.file.yaml.source;
    exports dev.xorcery.alchemy.file.yaml.transmute;

    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.extras;

    requires xorcery.reactivestreams.api;
    requires org.glassfish.hk2.api;

}