module xorcery.alchemy.file.excel {

    exports dev.xorcery.alchemy.file.excel.source;
    exports dev.xorcery.alchemy.file.excel.transmute;

    requires xorcery.alchemy.jar;
    requires org.dhatim.fastexcel;
    requires xorcery.reactivestreams.extras;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires org.dhatim.fastexcel.reader;
}