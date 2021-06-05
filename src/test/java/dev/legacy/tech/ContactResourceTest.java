package dev.legacy.tech;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;


@QuarkusTest
public class ContactResourceTest {


    @BeforeAll
    public static void setup() {
    }

    @Test
    public void testGetContactByIdEndpoint() {

        given()
                .when().log().all().get("/api/contact/1")
                .then()
                .log().all()
                .statusCode(200)
                .body("contact_id", is(1),
                        "first_name", is("Michael"),
                "last_name", is("Masters"));
    }

    @Test
    public void testGetContactCount() {
        given().when().log().all()
                .head("/api/contact")
                .then()
                .log().all()
                .statusCode(200)
                .header("X-Total-Contacts", "6");
    }

    @Test
    public void testGetAll() {
        given()
                .when().log().all()
                .get("/api/contact")
                .then()
                .statusCode(200)
                .body("$.size()", is(4),
                        "[0].contact_id", is(1),
                        "[1].first_name", is("Andrea"));
    }

    @Test
    public void testCreateContact() {
        given().queryParam("first_name", "Jackson")
                .queryParam("last_name", "Johnson")
                .queryParam("email_address", "jackson.johnson@europe.com")
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .post("/api/contact")
                .then().log().all()
                .header("Location", Matchers.matchesPattern("^http://localhost:8081/api/contact/.*"));
    }

    @Test
    public void testCreateContactFailed() {
        given()
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .post("/api/contact")
                .then().log().all()
                .statusCode(500);
    }
}
