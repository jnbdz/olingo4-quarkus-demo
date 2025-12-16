package com.sitenetsoft.demo;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class SmallPageSizeProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "demo.odata.max-page-size", "2"
        );
    }
}
