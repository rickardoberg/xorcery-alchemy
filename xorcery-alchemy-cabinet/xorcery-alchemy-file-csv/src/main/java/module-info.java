module xorcery.alchemy.file.csv {
    exports dev.xorcery.alchemy.file.csv.source;

    requires xorcery.reactivestreams.extras;
    requires com.opencsv;
    requires xorcery.alchemy.jar;
    requires xorcery.reactivestreams.api;
    requires org.glassfish.hk2.api;
}