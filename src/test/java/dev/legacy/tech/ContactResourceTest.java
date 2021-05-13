package dev.legacy.tech;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class ContactResourceTest {


    @BeforeAll
    public static void setup() {
//        ContactRepository mock = Mockito.mock(ContactRepository.class);
//        Contact contact = new Contact();
//        contact.setContactId(12345L);
//        contact.setFirstName("Michael");
//        contact.setLastName("Masters");
//        contact.setEmailAddr("mmasters@home.com");
//        Mockito.when(mock.findById(Mockito.anyLong())).thenReturn(contact);
//        QuarkusMock.installMockForType(mock, ContactRepository.class);
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
    public void testGetByCity() {
        given()
                .when()
                .queryParam("city", "Sacramento")
                .get("/api/contact")
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

    @Test
    public void testGetAll() {
        given()
                .when()
                .get("/api/contact")
                .then()
                .statusCode(200)
                .body("$.size()", is(3),
                        "[0].contact_id", is(1),
                        "[1].first_name", is("Andrea"));
    }
}
