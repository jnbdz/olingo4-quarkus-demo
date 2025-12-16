package com.sitenetsoft.demo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestProfile(SmallPageSizeProfile.class)
class OdataPagingLimitTest {

    @Test
    void serverMaxPageSize_shouldBeEnforced() {
        given()
                .accept(ContentType.JSON)
                .queryParam("$top", "5000")
                .queryParam("$format", "json")
            .when()
                .get("/odata/Products")
            .then()
                .statusCode(200)
                .body("value", hasSize(2))
                .body("$", hasKey("@odata.nextLink"));
    }
}
