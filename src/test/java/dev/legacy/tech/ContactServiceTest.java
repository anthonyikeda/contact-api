package dev.legacy.tech;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

@QuarkusTest
public class ContactServiceTest {

    @Inject
    ContactService contactService;

    @Test
    public void testGetContactById() {
        try {
            ContactDTO contact = this.contactService.getContactById(1L);
            Assertions.assertNotNull(contact);
            Assertions.assertEquals("Michael", contact.getFirstName());
            Assertions.assertNotNull(contact.getAddress());
        } catch (Exception e) {
            Assertions.assertNull(e);
        }
    }

    @Test
    public void testGetContactsPaginated() {
        try {
            List<ContactDTO> results = this.contactService.getPaginatedContacts(0, 10);

            Assertions.assertNotNull(results);
            Assertions.assertTrue(results.size() > 0);
            Assertions.assertEquals(3, results.size());
        } catch(Exception e) {
            Assertions.assertNull(e);
        }
    }
}
