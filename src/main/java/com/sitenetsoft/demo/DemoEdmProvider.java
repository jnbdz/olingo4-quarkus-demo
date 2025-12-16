package com.sitenetsoft.demo;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DemoEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "Demo";

    // Entity Types Names
    public static final String ET_PRODUCT_NAME = "Product";
    public static final FullQualifiedName ET_PRODUCT_FQN =
            new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

    // Entity Set Names
    public static final String ES_PRODUCTS_NAME = "Products";

    // Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER =
            new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Category
    public static final String ET_CATEGORY_NAME = "Category";
    public static final FullQualifiedName ET_CATEGORY_FQN =
            new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);

    @Override
    public List<CsdlSchema> getSchemas() {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(ET_PRODUCT_FQN));
        schema.setEntityTypes(entityTypes);

        // EntityContainer
        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);
        return schemas;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        if (ET_PRODUCT_FQN.equals(entityTypeName)) {
            // Key
            CsdlPropertyRef key = new CsdlPropertyRef().setName("ID");

            // Properties
            List<CsdlProperty> properties = new ArrayList<>();
            properties.add(new CsdlProperty()
                    .setName("ID")
                    .setType("Edm.Int32"));
            properties.add(new CsdlProperty()
                    .setName("Name")
                    .setType("Edm.String"));
            properties.add(new CsdlProperty()
                    .setName("Price")
                    .setType("Edm.Double"));

            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_PRODUCT_NAME);
            entityType.setKey(Collections.singletonList(key));
            entityType.setProperties(properties);

            return entityType;
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {
        if (CONTAINER.equals(entityContainer) && ES_PRODUCTS_NAME.equals(entitySetName)) {
            CsdlEntitySet entitySet = new CsdlEntitySet();
            entitySet.setName(ES_PRODUCTS_NAME);
            entitySet.setType(ET_PRODUCT_FQN);
            return entitySet;
        }
        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        CsdlEntitySet productsSet = new CsdlEntitySet()
                .setName(ES_PRODUCTS_NAME)
                .setType(ET_PRODUCT_FQN);

        CsdlEntityContainer container = new CsdlEntityContainer();
        container.setName(CONTAINER_NAME);
        container.setEntitySets(Collections.singletonList(productsSet));

        return container;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName name) {
        if (name == null || CONTAINER.equals(name)) {
            CsdlEntityContainerInfo info = new CsdlEntityContainerInfo();
            info.setContainerName(CONTAINER);
            return info;
        }
        return null;
    }
}
