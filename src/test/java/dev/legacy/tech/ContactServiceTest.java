package dev.legacy.tech;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ContactServiceTest {

    private final Logger log = LoggerFactory.getLogger(ContactServiceTest.class);

    @Inject
    ContactService contactService;

    @Test
    public void testUpdateContact() throws Exception {
        log.debug("testing update contact");

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
        log.debug("Testing create contact");
        Long contactId = contactService.createContact("Bob", "Marley", "bob.marley@home.com");
        assertThat(contactId).isNotNull();
    }

    @Test
    public void testGetContactAddressById() throws Exception {
        log.debug("Testing get contact address by id");
        ContactDTO contact = this.contactService.getContactById(1L);
        AddressDTO address = this.contactService.getContactAddressById(contact.getAddress().getAddressId());
        assertThat(address).isNotNull();
    }

    @Test
    public void testGetContactCount() {
        log.debug("Test get contact count");
        Long contactCount = this.contactService.getContactCount();
        assertThat(contactCount).isNotNull().isGreaterThan(1);
    }

    @Test
    public void testDeleteContactById() throws Exception {
        log.debug("Test delete contact by id");

        Long contactId = contactService.createContact("Bob", "Marley", "bob.marley@home.com");

        assertThat(contactId).isNotNull().isGreaterThan(1);

        log.debug("Contact created with id {}", contactId);
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

        try {
            ContactDTO contact = contactService.getContactById(contactId);
        } catch(ContactNotFoundException npe) {
            assertThat(npe).isNotNull();
        }

        try {
            AddressDTO failedAddress = contactService.getContactAddressById(addressId);
        } catch(NullPointerException npe) {
            assertThat(npe).isNotNull();
        }
    }

    @Test
    public void testGetContactAddresses() {
        log.debug("Test get contact addresses");
        List<AddressDTO> results = contactService.getContactAddresses(1L);

        assertThat(results).isNotNull().isNotEmpty();
    }

    @Test
    public void testCreateContactAddress() throws Exception {
        log.debug("Test create contact address");
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
    }

    @Test
    public void testGetContactById() {
        log.debug("Test get contact by id");
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
        log.debug("Test contacts paginated");
        try {
            List<ContactDTO> results = this.contactService.getPaginatedContacts(0, 10);
            assertThat(results).isNotNull().isNotEmpty();

            assertThat(results).hasSize(4);
        } catch(Exception e) {
            Assertions.assertNull(e);
        }
    }
}
