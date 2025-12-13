package com.sitenetsoft.demo;

import io.quarkus.test.junit.QuarkusTest;
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
                .body("Name", equalTo("Foo"))
                .body("Price", equalTo(10.0f)); // RestAssured treats numbers as float/double
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

}
