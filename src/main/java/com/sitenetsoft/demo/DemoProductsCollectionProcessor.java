package com.sitenetsoft.demo;

import java.net.URI;
import java.util.ArrayList;
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
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResourceProperty;

import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;

import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import org.apache.olingo.server.core.uri.queryoption.expression.BinaryImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.LiteralImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.MemberImpl;

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

        // 1) Load all products into a mutable list
        List<Product> products = new ArrayList<>(productRepository.findAll());

        // 2) Apply $filter (if present)
        FilterOption filterOption = uriInfo.getFilterOption();
        if (filterOption != null && filterOption.getExpression() != null) {
            products = applyFilter(filterOption.getExpression(), products);
        }

        // 3) Apply $orderby
        OrderByOption orderByOption = uriInfo.getOrderByOption();
        if (orderByOption != null &&
                orderByOption.getOrders() != null &&
                !orderByOption.getOrders().isEmpty()) {

            OrderByItem item = orderByOption.getOrders().get(0); // demo: only first item
            boolean desc = item.isDescending();

            Expression expr = item.getExpression();
            if (expr instanceof Member member) {
                UriInfoResource resourcePath = member.getResourcePath();

                String propName = null;
                if (resourcePath != null &&
                        !resourcePath.getUriResourceParts().isEmpty()) {

                    UriResource last = resourcePath.getUriResourceParts()
                            .get(resourcePath.getUriResourceParts().size() - 1);
                    if (last instanceof UriResourceProperty) {
                        propName = ((UriResourceProperty) last).getProperty().getName();
                    }
                }

                if (propName != null) {
                    final String sortProp = propName;

                    products.sort((a, b) -> {
                        if ("ID".equals(sortProp)) {
                            return Integer.compare(a.getID(), b.getID());
                        } else if ("Name".equals(sortProp)) {
                            return a.getName().compareToIgnoreCase(b.getName());
                        } else if ("Price".equals(sortProp)) {
                            return Double.compare(a.getPrice(), b.getPrice());
                        }
                        // Unknown property → keep original order
                        return 0;
                    });

                    if (desc) {
                        java.util.Collections.reverse(products);
                    }
                }
            }
        }

        // 4) Apply $skip and $top (pagination)
        SkipOption skipOption = uriInfo.getSkipOption();
        TopOption topOption   = uriInfo.getTopOption();

        int skip = (skipOption == null) ? 0 : skipOption.getValue();
        int top  = (topOption == null)   ? products.size() : topOption.getValue();

        int fromIndex = Math.min(skip, products.size());
        int toIndex   = Math.min(fromIndex + top, products.size());

        List<Product> paged = products.subList(fromIndex, toIndex);

        // 5) Handle $select (projection)
        SelectOption selectOption = uriInfo.getSelectOption();
        boolean selectAll = (selectOption == null || selectOption.getSelectItems().isEmpty());

        boolean includeId    = true;
        boolean includeName  = true;
        boolean includePrice = true;

        if (!selectAll) {
            boolean star = selectOption.getSelectItems().stream().anyMatch(SelectItem::isStar);
            if (star) {
                // $select=* → all properties
                includeId = includeName = includePrice = true;
            } else {
                includeId = includeName = includePrice = false;
                for (SelectItem item : selectOption.getSelectItems()) {
                    UriInfoResource path = item.getResourcePath();
                    if (path == null || path.getUriResourceParts().isEmpty()) {
                        continue;
                    }
                    UriResource last = path.getUriResourceParts()
                            .get(path.getUriResourceParts().size() - 1);
                    if (last instanceof UriResourceProperty urp) {
                        String prop = urp.getProperty().getName();
                        if ("ID".equals(prop)) {
                            includeId = true;
                        } else if ("Name".equals(prop)) {
                            includeName = true;
                        } else if ("Price".equals(prop)) {
                            includePrice = true;
                        }
                    }
                }
            }
        }

        // 6) Map to OData EntityCollection with projection
        EntityCollection entityCollection = new EntityCollection();

        for (Product p : paged) {
            Entity e = new Entity();
            if (includeId) {
                e.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, p.getID()));
            }
            if (includeName) {
                e.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, p.getName()));
            }
            if (includePrice) {
                e.addProperty(new Property(null, "Price", ValueType.PRIMITIVE, p.getPrice()));
            }

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

    // ---- $filter helpers ----

    private List<Product> applyFilter(Expression expr, List<Product> input)
            throws ODataApplicationException {

        if (!(expr instanceof BinaryImpl bin)) {
            throw new ODataApplicationException(
                    "Only simple binary filters are supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        Expression left  = bin.getLeftOperand();
        Expression right = bin.getRightOperand();
        BinaryOperatorKind op = bin.getOperator();

        if (!(left instanceof MemberImpl) || !(right instanceof LiteralImpl)) {
            throw new ODataApplicationException(
                    "Filter must be: Property op Literal",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        MemberImpl member = (MemberImpl) left;
        UriInfoResource path = member.getResourcePath();
        String propName = ((UriResourceProperty)
                path.getUriResourceParts().get(path.getUriResourceParts().size() - 1)
        ).getProperty().getName();

        String raw = unquote(((LiteralImpl) right).getText());

        return filterByOperator(input, propName, op, raw);
    }

    private List<Product> filterByOperator(List<Product> items, String prop,
                                           BinaryOperatorKind op, String raw)
            throws ODataApplicationException {

        List<Product> out = new ArrayList<>();

        for (Product p : items) {
            if (matches(p, prop, op, raw)) {
                out.add(p);
            }
        }

        return out;
    }

    private boolean matches(Product p, String prop, BinaryOperatorKind op, String raw)
            throws ODataApplicationException {

        try {
            return switch (prop) {
                case "ID"    -> compareInt(p.getID(), op, Integer.parseInt(raw));
                case "Price" -> compareDouble(p.getPrice(), op, Double.parseDouble(raw));
                case "Name"  -> op == BinaryOperatorKind.EQ && p.getName().equals(raw);
                default      -> false;
            };
        } catch (NumberFormatException ex) {
            throw new ODataApplicationException(
                    "Invalid literal in $filter: " + raw,
                    HttpStatusCode.BAD_REQUEST.getStatusCode(),
                    Locale.ENGLISH
            );
        }
    }

    private boolean compareInt(int left, BinaryOperatorKind op, int right) {
        return switch (op) {
            case EQ -> left == right;
            case GT -> left > right;
            case LT -> left < right;
            default -> false;
        };
    }

    private boolean compareDouble(double left, BinaryOperatorKind op, double right) {
        return switch (op) {
            case EQ -> Double.compare(left, right) == 0;
            case GT -> left > right;
            case LT -> left < right;
            default -> false;
        };
    }

    private String unquote(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() >= 2 && text.startsWith("'") && text.endsWith("'")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
