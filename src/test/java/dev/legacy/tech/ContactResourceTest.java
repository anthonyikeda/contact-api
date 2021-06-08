package dev.legacy.tech;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.assertj.core.api.Assertions.*;


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
    public void testGetContactByIdNotFound() {
        given().when().log().all()
                .get("/api/contact/100")
                .then()
                .log().all()
                .statusCode(404);
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
                .body("$.size()", is(5),
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

    @Test
    public void testUpdateFoundContactButSameParams() {
        given()
                .queryParam("first_name", "Michael")
                .queryParam("last_name", "Masters")
                .queryParam("email_address", "mmasters@home.com")
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .put("/api/contact/1")
                .then()
                .statusCode(304);
    }

    @Test
    public void testUpdateContact() {
    given()
            .queryParam("first_name", "michelle")
            .queryParam("last_name", "masters")
            .queryParam("email_address", "mm@master.com")
            .contentType(ContentType.fromContentType("application/json"))
            .when().log().all()
            .put("/api/contact/1")
            .then()
            .statusCode(202);
    }

    @Test
    public void testUpdateNonExistentContact() {
        given()
                .queryParam("first_name", "michelle")
                .queryParam("last_name", "masters")
                .queryParam("email_address", "mm@master.com")
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .put("/api/contact/9999")
                .then()
                .statusCode(404)
                .header("X-Not-Found", "Client");
    }

    @Test
    public void testUpdateContactNoFields() {
        given()
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .put("/api/contact/2")
                .then()
                .statusCode(304);
    }

    @Test
    public void testCreateContactAddress() {

        Response response = given().queryParam("first_name", "Judy")
                .queryParam("last_name", "Juice")
                .queryParam("email_addr", "judy.juice@sauce.com")
                .when()
                .post("/api/contact");
        String location = response.getHeader("Location");
        int statusCode = response.getStatusCode();
        assertThat(statusCode).isEqualTo(201);

        AddressDTO address = new AddressDTO();
        address.setStreet1("25 Gorad St");
        address.setStreet2("Apt 22");
        address.setCity("Gundagai");
        address.setState("New South Wales");
        address.setCountry("Australia");
        address.setPostalCode("2436");

        given()
                .body(address)
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .post(String.format("%s/address", location))
                .then().log().all()
                .statusCode(201)
                .header("Location", Matchers.matchesPattern("^(http:\\/\\/localhost:8081\\/api\\/contact\\/)\\d+(\\/address\\/)\\d+$"));
    }

    @Test
    public void testCreateNonContactAddress() {
        AddressDTO address = new AddressDTO();
        address.setStreet1("25 Gorad St");
        address.setStreet2("Apt 22");
        address.setCity("Gundagai");
        address.setState("New South Wales");
        address.setCountry("Australia");
        address.setPostalCode("2436");

        given()
                .body(address)
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .post("/api/contact/54432/address")
                .then().log().all()
                .statusCode(404);
    }

    @Test
    public void testCreateContactDuplicateAddress() {
        AddressDTO address = new AddressDTO();
        address.setStreet1("25 Gorad St");
        address.setStreet2("Apt 22");
        address.setCity("Gundagai");
        address.setState("New South Wales");
        address.setCountry("Australia");
        address.setPostalCode("2436");

        given()
                .body(address)
                .contentType(ContentType.fromContentType("application/json"))
                .when().log().all()
                .post("/api/contact/1/address")
                .then().log().all()
                .statusCode(400);
    }
}
