package dev.ldv;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class InfoResourceTest {

    @Test
    void testInfoEndpoint() {
        given()
                .when().get("/api/info")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", is("book-catalog-test"))
                .body("framework", is("Quarkus"));
    }
}
