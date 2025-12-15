package com.sitenetsoft.demo;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.CountEntityCollectionProcessor;
import org.apache.olingo.server.api.uri.UriInfo;

import org.apache.olingo.server.api.serializer.SerializerException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class DemoProductsCountProcessor implements CountEntityCollectionProcessor {

    private final ProductRepository repo;

    private OData odata;
    private ServiceMetadata serviceMetadata;

    public DemoProductsCountProcessor(ProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    // required because CountEntityCollectionProcessor extends EntityCollectionProcessor (in your version)
    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        // Delegate to your existing collection processor
        DemoProductsCollectionProcessor delegate = new DemoProductsCollectionProcessor(repo);
        delegate.init(odata, serviceMetadata);
        delegate.readEntityCollection(request, response, uriInfo, responseFormat);
    }

    @Override
    public void countEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
            throws ODataApplicationException {

        int count = repo.findAll().size();

        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader("Content-Type", "text/plain");
        response.setContent(new ByteArrayInputStream(
                String.valueOf(count).getBytes(StandardCharsets.UTF_8)
        ));
    }
}