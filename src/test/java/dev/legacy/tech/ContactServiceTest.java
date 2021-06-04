package dev.legacy.tech;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

@QuarkusTest
public class ContactServiceTest {

    private final Logger log = LoggerFactory.getLogger(ContactServiceTest.class);

    @Inject
    ContactService contactService;

    @Test
    public void testUpdateContact() throws Exception {

        Long contactId = this.contactService.createContact("Mary", "Contrary", "mcontrary@mail.com");
        ContactDTO contact = this.contactService.getContactById(contactId);

        assertThat(contact).isNotNull();

        contact.setEmailAddr("mary@apple.com");
        this.contactService.updateContact(contact);

        ContactDTO updated = this.contactService.getContactById(contactId);

        assertThat(updated).isNotNull().hasFieldOrPropertyWithValue("emailAddr", "mary@apple.com");

    }

    @Test
    public void testCreateContact() {
        try  {
            Long contactId = contactService.createContact("Bob", "Marley", "bob.marley@home.com");
            assertThat(contactId).isNotNull();
        } catch(SQLException sqle) {
            log.error("Error creating contact in test", sqle);
        }
    }

    @Test
    public void getContactAddressById() throws Exception {
        AddressDTO address = this.contactService.getContactAddressById(14L);
        assertThat(address).isNotNull();
    }

    @Test
    public void testGetContactCount() throws Exception {
        Long contactCount = this.contactService.getContactCount();
        assertThat(contactCount).isNotNull().isGreaterThan(1);
    }

    @Test
    public void testDeleteContactById() throws Exception {

        Long contactId = contactService.createContact("Bob", "Marley", "bob.marley@home.com");

        assertThat(contactId).isNotNull().isGreaterThan(1);

        AddressDTO address = new AddressDTO();
        address.setContactId(contactId);
        address.setStreet1("123 Sesame St");
        address.setStreet2("Apt 23");
        address.setCity("New York");
        address.setState("New York");
        address.setCountry("USA");
        address.setPostalCode("123456");

        Long addressId = this.contactService.createContactAddress(contactId, address);
        assertThat(addressId).isNotNull();

        contactService.deleteContactById(contactId);

        ContactDTO contact = contactService.getContactById(contactId);
        assertThat(contact).isNull();

        AddressDTO failedAddress = contactService.getContactAddressById(addressId);
        assertThat(failedAddress).isNull();
    }

    @Test
    public void testGetContactAddresses() throws Exception {
        List<AddressDTO> results = contactService.getContactAddresses(1L);

        assertThat(results).isNotNull().isNotEmpty();
    }

    @Test
    public void testCreateContactAddress() {
        try  {
            Long contactId = contactService.createContact("Bob", "Marley", "bob.marley@home.com");
            assertThat(contactId).isNotNull();
            AddressDTO address = new AddressDTO();
            address.setContactId(contactId);
            address.setStreet1("123 Sesame St");
            address.setStreet2("Apt 23");
            address.setCity("New York");
            address.setState("New York");
            address.setCountry("USA");
            address.setPostalCode("123456");

            Long addressId = contactService.createContactAddress(contactId, address);
            assertThat(addressId).isNotNull().isGreaterThan(1);
        } catch(SQLException sqle) {
            log.error("Error creating contact in test", sqle);
        }


    }

    @Test
    public void testGetContactById() {
        try {
            ContactDTO contact = this.contactService.getContactById(1L);
            Assertions.assertNotNull(contact);
            Assertions.assertEquals("Michael", contact.getFirstName());
//            Assertions.assertNotNull(contact.getAddress());
        } catch (Exception e) {
            Assertions.assertNull(e);
        }
    }

    @Test
    public void testGetContactsPaginated() {
        try {
            List<ContactDTO> results = this.contactService.getPaginatedContacts(0, 10);
            assertThat(results).isNotNull().isNotEmpty();

            assertThat(results).hasSize(4);
        } catch(Exception e) {
            Assertions.assertNull(e);
        }
    }
}
