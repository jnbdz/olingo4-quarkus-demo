package com.sitenetsoft.demo;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
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
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

public class DemoProductsCollectionProcessor implements EntityCollectionProcessor {

    private final ProductRepository productRepository;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public DemoProductsCollectionProcessor(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(
            ODataRequest request,
            ODataResponse response,
            UriInfo uriInfo,
            ContentType responseFormat
    ) throws ODataApplicationException, SerializerException {

        UriResource first = uriInfo.getUriResourceParts().get(0);
        UriResourceEntitySet es = (UriResourceEntitySet) first;

        if (!"Products".equals(es.getEntitySet().getName())) {
            throw new ODataApplicationException(
                    "Only Products entity set is supported in this demo",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        // 1) Load all products
        List<Product> products = productRepository.findAll();

        // 2) Apply $skip and $top (pagination)
        SkipOption skipOption = uriInfo.getSkipOption();
        TopOption topOption = uriInfo.getTopOption();

        int skip = (skipOption == null) ? 0 : skipOption.getValue();
        int top = (topOption == null) ? products.size() : topOption.getValue();

        int fromIndex = Math.min(skip, products.size());
        int toIndex = Math.min(fromIndex + top, products.size());

        List<Product> paged = products.subList(fromIndex, toIndex);

        // 3) Map to OData EntityCollection
        EntityCollection entityCollection = new EntityCollection();

        for (Product p : paged) {
            Entity e = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, p.getID()))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, p.getName()))
                    .addProperty(new Property(null, "Price", ValueType.PRIMITIVE, p.getPrice()));

            e.setId(URI.create("Products(" + p.getID() + ")"));
            entityCollection.getEntities().add(e);
        }

        EdmEntityType entityType = es.getEntitySet().getEntityType();

        ContentType actualFormat = responseFormat;
        if (responseFormat != null && responseFormat.isCompatible(ContentType.APPLICATION_JSON)) {
            actualFormat = ContentType.parse("application/json;odata.metadata=none");
        }

        ODataSerializer serializer = odata.createSerializer(actualFormat);

        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions
                .with()
                .build();

        SerializerResult result = serializer.entityCollection(
                serviceMetadata,
                entityType,
                entityCollection,
                opts
        );

        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setContent(result.getContent());
        response.setHeader("Content-Type", actualFormat.toContentTypeString());
    }
}
