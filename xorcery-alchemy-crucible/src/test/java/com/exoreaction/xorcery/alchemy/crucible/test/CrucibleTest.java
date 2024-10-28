package com.exoreaction.xorcery.alchemy.crucible.test;

import com.exoreaction.xorcery.alchemy.crucible.Crucible;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

class CrucibleTest {

    @Test
    public void testCrucible() throws Exception {
        try (Xorcery crucible = new Xorcery(new ConfigurationBuilder().addTestDefaults()
                .addYaml("""
crucible:
  recipes:
    - name: "test"
      source:
        jar: yaml
        file: "foo.yaml"
      transmutes:
      - name: "addtimestamp"
        jar: jslt
        jslt: "{{ RESOURCE.string.transform.jslt }}"
      result:
        jar: log
                        """)
                .build()))
        {
            crucible.getServiceLocator().getService(Crucible.class).getResult().join();
        }
    }
}