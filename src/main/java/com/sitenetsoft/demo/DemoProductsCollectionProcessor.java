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

import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import org.apache.olingo.server.core.uri.queryoption.expression.BinaryImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.LiteralImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.MemberImpl;

import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;

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

        var countOption = uriInfo.getCountOption();
        boolean includeCount = (countOption != null && Boolean.TRUE.equals(countOption.getValue()));

        // after filter, before skip/top:
        int countAfterFilter = products.size();

        // 3) Apply $orderby
        OrderByOption orderByOption = uriInfo.getOrderByOption();
        if (orderByOption != null && orderByOption.getOrders() != null && !orderByOption.getOrders().isEmpty()) {

            java.util.Comparator<Product> comparator = null;

            for (OrderByItem item : orderByOption.getOrders()) {
                String propName = extractOrderByProp(item);
                if (propName == null) continue;

                java.util.Comparator<Product> c = switch (propName) {
                    case "ID" -> java.util.Comparator.comparingInt(Product::getID);
                    case "Name" -> java.util.Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
                    case "Price" -> java.util.Comparator.comparingDouble(Product::getPrice);
                    default -> null;
                };

                if (c == null) continue;
                if (item.isDescending()) c = c.reversed();

                comparator = (comparator == null) ? c : comparator.thenComparing(c);
            }

            if (comparator != null) {
                products.sort(comparator);
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

        // 5) Map to OData EntityCollection with projection
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

        SelectOption selectOption = uriInfo.getSelectOption();

        EntityCollectionSerializerOptions.Builder builder = EntityCollectionSerializerOptions.with();
        if (selectOption != null) builder.select(selectOption);
        if (countOption != null)  builder.count(countOption);

        EntityCollectionSerializerOptions opts = builder.build();

        if (includeCount) {
            entityCollection.setCount(countAfterFilter);
        }

        ODataSerializer serializer = odata.createSerializer(actualFormat);

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

    private String extractOrderByProp(OrderByItem item) {
        Expression expr = item.getExpression();
        if (!(expr instanceof Member member)) return null;
        UriInfoResource path = member.getResourcePath();
        if (path == null || path.getUriResourceParts().isEmpty()) return null;
        UriResource last = path.getUriResourceParts().get(path.getUriResourceParts().size() - 1);
        if (!(last instanceof UriResourceProperty p)) return null;
        return p.getProperty().getName();
    }

    // ---- $filter helpers ----

    private List<Product> applyFilter(Expression expr, List<Product> input)
            throws ODataApplicationException {

        List<Product> out = new ArrayList<>();
        for (Product p : input) {
            if (evalBoolean(expr, p)) {
                out.add(p);
            }
        }
        return out;
    }

    private boolean evalBoolean(Expression expr, Product p) throws ODataApplicationException {
        if (!(expr instanceof Binary bin)) {
            throw new ODataApplicationException(
                    "Only binary expressions are supported in this demo",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        BinaryOperatorKind op = bin.getOperator();

        // Logical
        if (op == BinaryOperatorKind.AND) {
            return evalBoolean(bin.getLeftOperand(), p) && evalBoolean(bin.getRightOperand(), p);
        }
        if (op == BinaryOperatorKind.OR) {
            return evalBoolean(bin.getLeftOperand(), p) || evalBoolean(bin.getRightOperand(), p);
        }

        // Comparisons: (Member op Literal)
        Expression left = bin.getLeftOperand();
        Expression right = bin.getRightOperand();

        if (!(left instanceof Member member) || !(right instanceof Literal lit)) {
            throw new ODataApplicationException(
                    "Filter must be: Property op Literal (or combined with and/or)",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(),
                    Locale.ENGLISH
            );
        }

        String prop = extractPropName(member);
        String raw = unquote(lit.getText());

        return matches(p, prop, op, raw);
    }

    private String extractPropName(Member member) throws ODataApplicationException {
        UriInfoResource resourcePath = member.getResourcePath();
        if (resourcePath == null || resourcePath.getUriResourceParts().isEmpty()) {
            throw new ODataApplicationException("Invalid member in $filter",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        UriResource last = resourcePath.getUriResourceParts().get(resourcePath.getUriResourceParts().size() - 1);
        if (!(last instanceof UriResourceProperty prop)) {
            throw new ODataApplicationException("Only property members are supported in $filter",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        return prop.getProperty().getName();
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
