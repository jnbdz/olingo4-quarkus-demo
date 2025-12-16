package com.sitenetsoft.demo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class OdataEndpointTest {

    @Test
    void serviceDocument_shouldExposeProductsCollection() {
        given()
                .accept("application/xml")
                .when()
                .get("/odata/")
                .then()
                .statusCode(200)
                .body(containsString("Demo.Container"))
                .body(containsString("Products"));
    }

    @Test
    void metadata_shouldExposeProductEntityType() {
        given()
                .accept("application/xml")
                .when()
                .get("/odata/$metadata")
                .then()
                .statusCode(200)
                .body(containsString("EntityType Name=\"Product\""))
                .body(containsString("EntitySet Name=\"Products\""));
    }

    @Test
    void products_shouldReturnSampleJsonArray() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                // OData-specific header – replaces the @odata.context check
                .header("OData-Version", equalTo("4.0"))
                .contentType(startsWith("application/json"))
                // Now focus on the payload we care about
                .body("value", hasSize(3))
                .body("value.ID", containsInAnyOrder(1, 2, 3))
                .body("value.Name", hasItems("Foo", "Bar", "Baz"));
    }

    @Test
    public void productById_shouldReturnSingleJson() {
        given()
                .accept("application/json")
                .when()
                .get("/odata/Products(1)?$format=json")
                .then()
                .statusCode(200)
                .body("ID", equalTo(1))
                .body("Name", equalTo("Product 1"))
                .body("Price", equalTo(1.25f));
    }

    @Test
    void queryOptions_shouldWorkTogether() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$select", "Name")
                .queryParam("$filter", "Price gt 15")
                .queryParam("$orderby", "Name desc")
                .queryParam("$top", "1")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(1))
                .body("value[0].Name", equalTo("Baz"));
    }

    @Test
    void paging_shouldWorkWithSkipAndTop() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$orderby", "ID asc")
                .queryParam("$skip", "1")
                .queryParam("$top", "1")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(1))
                .body("value[0].ID", equalTo(2))
                .body("value[0].Name", equalTo("Bar"));
    }

    @Test
    void select_shouldWorkOnSingleEntity() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$select", "Name")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products(1)")
                .then()
                .statusCode(200)
                .body("ID", equalTo(1))
                .body("Name", equalTo("Foo"))
                .body("$", not(hasKey("Price")));
    }

    @Test
    void filterById_shouldReturnSingleMatch() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$filter", "ID eq 2")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(1))
                .body("value[0].ID", equalTo(2))
                .body("value[0].Name", equalTo("Bar"));
    }

    @Test
    void orderByPriceDesc_shouldReturnMostExpensiveFirst() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$orderby", "Price desc")
                .queryParam("$top", "1")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(1))
                .body("value[0].Name", equalTo("Product 50"))
                .body("value[0].Price", equalTo(62.5f));
    }

    @Test
    void invalidFilterLiteral_shouldReturn400() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$filter", "Price gt 'nope'")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(400)
                .body("error", notNullValue())
                .body("error.message", anyOf(
                        containsString("type"),
                        containsString("Type"),
                        containsString("Incompatible")
                ));
    }

    @Test
    void countQueryOption_shouldReturnCountAnnotation() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$count", "true")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("get('@odata.count')", equalTo(3))
                .body("value", hasSize(3));
    }

    @Test
    void countSegment_shouldReturnPlainNumber() {
        given()
                .accept(ContentType.TEXT)
                .when()
                .get("/odata/Products/$count")
                .then()
                .statusCode(200)
                .body(equalTo("3"));
    }

    @Test
    void filterWithAnd_shouldWork() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$filter", "Price gt 2 and ID lt 3")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(1))
                .body("value[0].ID", equalTo(2))
                .body("value[0].Name", equalTo("Product 2"));
    }

    @Test
    void filterWithOr_shouldWork() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$filter", "ID eq 1 or ID eq 3")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(2))
                .body("value.ID", containsInAnyOrder(1, 3));
    }

    @Test
    void filterWithParentheses_shouldRespectPrecedence() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$filter", "(ID eq 1 or ID eq 2) and Price lt 15")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(1))
                .body("value[0].Name", equalTo("Foo"));
    }

    @Test
    void selectMultipleProperties_shouldReturnOnlyThose() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$select", "Name,Price")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products(1)")
                .then()
                .statusCode(200)
                .body("ID", equalTo(1))         // selected properties + key
                .body("Name", equalTo("Foo"))
                .body("Price", equalTo(10.0f));
    }

    @Test
    void orderByMultipleFields_shouldWork() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$orderby", "Price desc,Name asc")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value[0].Name", equalTo("Baz"))
                .body("value[1].Name", equalTo("Bar"))
                .body("value[2].Name", equalTo("Foo"));
    }

    @Test
    void topZero_shouldReturnEmptyCollection() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$top", "0")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(0));
    }

    @Test
    void skipBeyondSize_shouldReturnEmptyCollection() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$skip", "100")
                .queryParam("$format", "json")
                .when()
                .get("/odata/Products")
                .then()
                .statusCode(200)
                .body("value", hasSize(0));
    }

}