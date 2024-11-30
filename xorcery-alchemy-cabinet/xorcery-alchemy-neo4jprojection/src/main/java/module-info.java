module xorcery.alchemy.neo4jprojection {
    exports dev.xorcery.alchemy.neo4jprojection.result;
    exports dev.xorcery.alchemy.neo4jprojection.source;

    requires xorcery.alchemy.jar;
    requires xorcery.neo4j.projections;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires xorcery.domainevents.api;

}