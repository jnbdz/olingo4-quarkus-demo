package com.sitenetsoft.demo;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        org.apache.olingo.server.core.ODataImpl.class
})
public class OlingoNativeReflection {
}
