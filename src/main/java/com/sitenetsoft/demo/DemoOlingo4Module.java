package com.sitenetsoft.demo;

import com.sitenetsoft.olingo4.quarkus.runtime.Olingo4Module;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.server.api.ODataHttpHandler;

@ApplicationScoped
public class DemoOlingo4Module implements Olingo4Module {

    @Inject
    ProductRepository productRepository;

    @Override
    public CsdlEdmProvider edmProvider() {
        // DemoEdmProvider extends CsdlAbstractEdmProvider, which is a CsdlEdmProvider
        return new DemoEdmProvider();
    }

    @Override
    public void registerProcessors(ODataHttpHandler handler) {
        // OLD:
        // handler.register(new DemoEntityCollectionProcessor(productRepository));

        // NEW:
        handler.register(new DemoProductsCollectionProcessor(productRepository));
        handler.register(new DemoEntityProcessor(productRepository));
        handler.register(new DemoProductsCountProcessor(productRepository));
    }
}
