package com.exoreaction.xorcery.alchemy.runner.test;

import com.exoreaction.xorcery.alchemy.runner.Alchemist;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

class AlchemistTest {

    @Test
    public void testAlchemist() throws Exception {
        try (Xorcery alchemist = new Xorcery(new ConfigurationBuilder().addTestDefaults()
                .addYaml("""
alchemist:
  transmutations:
    - name: "test"
      input:
        plugin: yaml
        file: "foo.yaml"
      transmutes:
      - name: "addtimestamp"
        plugin: jslt
        jslt: "{{ RESOURCE.string.transform.jslt }}"  
      output:
        plugin: log
                        """)
                .build()))
        {
            alchemist.getServiceLocator().getService(Alchemist.class).getResult().join();
        }
    }
}