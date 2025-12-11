package com.sitenetsoft.demo;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

public class DemoEntityProcessor implements EntityProcessor {

    private final ProductRepository productRepository;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public DemoEntityProcessor(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntity(
            ODataRequest request,
            ODataResponse response,
            UriInfo uriInfo,
            ContentType responseFormat
    ) throws ODataApplicationException, SerializerException {

        // Expect /Products(<key>)
        UriResource first = uriInfo.getUriResourceParts().get(0);
        UriResourceEntitySet es = (UriResourceEntitySet) first;

        if (es.getKeyPredicates().isEmpty()) {
            throw new ODataApplicationException(
                    "Missing key predicate",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        UriParameter keyParam = es.getKeyPredicates().get(0);
        String keyText = keyParam.getText(); // "1" for numeric key

        int id;
        try {
            id = Integer.parseInt(keyText);
        } catch (NumberFormatException e) {
            throw new ODataApplicationException(
                    "Invalid key: " + keyText,
                    HttpStatusCode.BAD_REQUEST.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        Optional<Product> maybe = productRepository.findById(id);
        if (maybe.isEmpty()) {
            throw new ODataApplicationException(
                    "Product not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        Product p = maybe.get();

        Entity e = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, p.getID()))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, p.getName()))
                .addProperty(new Property(null, "Price", ValueType.PRIMITIVE, p.getPrice()));

        e.setId(URI.create("Products(" + p.getID() + ")"));

        EdmEntityType entityType = es.getEntitySet().getEntityType();

        ContentType actualFormat = responseFormat;
        if (responseFormat != null && responseFormat.isCompatible(ContentType.APPLICATION_JSON)) {
            actualFormat = ContentType.parse("application/json;odata.metadata=none");
        }

        ODataSerializer serializer = odata.createSerializer(actualFormat);
        EntitySerializerOptions opts = EntitySerializerOptions.with().build();

        SerializerResult result = serializer.entity(
                serviceMetadata,
                entityType,
                e,
                opts
        );

        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setContent(result.getContent());
        response.setHeader("Content-Type", actualFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response,
                             UriInfo uriInfo, ContentType requestFormat,
                             ContentType responseFormat) {
        throw new UnsupportedOperationException("Not implemented in demo");
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response,
                             UriInfo uriInfo, ContentType requestFormat,
                             ContentType responseFormat) {
        throw new UnsupportedOperationException("Not implemented in demo");
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response,
                             UriInfo uriInfo) {
        throw new UnsupportedOperationException("Not implemented in demo");
    }
}
