package dev.legacy.tech;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/contact")
public class ContactResource {

    private final Logger log = LoggerFactory.getLogger(ContactResource.class);

    @Inject
    MeterRegistry registry;

    @Inject
    ContactService contactService;

    @Inject
    ContactRepository repository;

    @Inject
    AddressRepository addressRepository;

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContactCount() {
        long total = repository.count();
        return Response.ok().header("X-Total-Contacts", total).build();
    }

    @Path("/{contactId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContactById(@PathParam("contactId") Long contactId) {
        Contact contact = repository.findById(contactId);

        if(contact != null) {
            ContactDTO dto = new ContactDTO();
            dto.setContactId(contact.getContactId());
            dto.setEmailAddr(contact.getEmailAddr());
            dto.setFirstName(contact.getFirstName());
            dto.setLastName(contact.getLastName());
            return Response.ok(dto).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findContactsPaginated(@DefaultValue("0") @QueryParam("start") Integer start,
                                       @DefaultValue("10") @QueryParam("size") Integer size) {
        List<Contact> contacts;
        log.debug("Querying for all contacts with start {} and size {}", start, size);
        Timer.Sample queryTimer = Timer.start(registry);
        contacts = repository.find("select c from Contact c").page(start, size).list();
        queryTimer.stop(registry.timer("contacts.all.panache.query"));

        Timer.Sample transformTime = Timer.start();
        List<ContactDTO> results = contacts.stream().map(contact -> {
            ContactDTO contactDTO = new ContactDTO();
            contactDTO.setContactId(contact.getContactId());
            contactDTO.setEmailAddr(contact.getEmailAddr());
            contactDTO.setFirstName(contact.getFirstName());
            contactDTO.setLastName(contact.getLastName());

            AddressDTO addrDTO = new AddressDTO();
            Address address = contact.getAddress();
            addrDTO.setAddressId(address.getAddressId());
            addrDTO.setStreet1(address.getStreet1());
            addrDTO.setStreet2(address.getStreet2());
            addrDTO.setCity(address.getCity());
            addrDTO.setState(address.getState());
            addrDTO.setCountry(address.getCountry());
            addrDTO.setPostalCode(address.getPostalCode());
            contactDTO.setAddress(addrDTO);

            return contactDTO;
        }).collect(Collectors.toList());

        transformTime.stop(registry.timer("contacts.all.transform"));
        return Response.ok(results).build();
    }

    @PUT
    @Path("/{contact_id}")
    @Consumes(value={"application/json"})
    @Produces(value={"application/json"})
    @Transactional
    public Response updateContact(@PathParam("contact_id") Long contactId, @QueryParam("first_name") String firstName, @QueryParam("last_name") String lastname,
                                  @QueryParam("email_address") String emailAddress) {
        Contact contact = repository.findById(contactId);
        if(contact == null) {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
        }

        boolean updated = false;

        if(firstName != null && !firstName.equals(contact.getFirstName())) {
            log.debug("Updating firstname: {}", firstName);
            contact.setFirstName(firstName);
            updated = true;
        }

        if(lastname != null && !lastname.equals(contact.getLastName())) {
            log.debug("Updating lastname: {}", lastname);
            contact.setLastName(lastname);
            updated = true;
        }

        if(emailAddress != null && !emailAddress.equals(contact.getEmailAddr())) {
            log.debug("Updating emailAddress: {}", emailAddress);
            contact.setEmailAddr(emailAddress);
            updated = true;
        }

        if(updated) {
            log.debug("Saving changed Contact {}", contactId);
            repository.persist(contact);
            return Response.accepted().build();
        }

        return Response.notModified().build();
    }

    @POST
    @Path("")
    @Transactional
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response createContact(@QueryParam("first_name") String firstName, @QueryParam("last_name") String lastname,
                                  @QueryParam("email_address") String emailAddress) {
        Contact contact = new Contact();
        contact.setFirstName(firstName);
        contact.setLastName(lastname);
        contact.setEmailAddr(emailAddress);
        repository.persist(contact);
        log.debug("Saved contact, new id is: {}", contact.getContactId());
        return Response.created(URI.create(String.format("/api/contact/%d", contact.getContactId()))).build();
    }

    @POST
    @Path("/{contact_id}/address")
    @Transactional
    @Consumes("application/json")
    @Produces(value = {"application/json"})
    public Response createContactAddress(@PathParam("contact_id") Long contactId, AddressDTO addressDTO) {

        Contact contact = repository.findById(contactId);

        if(contact == null) {
            log.debug("Contact {} does not exist", contactId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<Address> addresses = addressRepository.list("select a from Address a where a.contactId = ?1", contactId);
        if(addresses.size() == 0) {
            Address address = new Address();
            address.setContact(contact);
            address.setStreet1(addressDTO.getStreet1());
            address.setStreet2(addressDTO.getStreet2());
            address.setCity(addressDTO.getCity());
            address.setState(addressDTO.getState());
            address.setCountry(addressDTO.getCountry());
            address.setPostalCode(addressDTO.getPostalCode());

            addressRepository.persist(address);
            log.debug("Saved address {} for contact {}", address.getAddressId(), contactId);
            return Response.created(URI.create(String.format("/api/contact/%d/address%d", contactId, address.getAddressId()))).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Contact already has an address with id %d", addresses.get(0).getAddressId())).build();
        }
    }

    @PUT
    @Path("/{contact_id}/address/{address_id}")
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response updateContactAddress(@PathParam("contact_id") Long contactId, @PathParam("address_id") Long addressId,
                                         @QueryParam("street_1") String street1, @QueryParam("street_2") String street2,
                                         @QueryParam("city") String city, @QueryParam("state") String state,
                                         @QueryParam("country") String country, @QueryParam("postal_code") String postalCode) {
        Contact contact = repository.findById(contactId);

        if(contact == null) {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Contact").build();
        }

        if(contact.getAddress() == null) {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Address").build();
        }

        Address address = addressRepository.findById(addressId);

        boolean updated = false;

        if(street1 != null && !street1.equals(address.getStreet1())) {
            address.setStreet1(street1);
            updated = true;
        }

        if(street2 != null && !street2.equals(address.getStreet2())) {
            address.setStreet2(street2);
            updated = true;
        }

        if(city != null && !city.equals(address.getCity())) {
            address.setCity(city);
            updated = true;
        }

        if(state != null && !state.equals(address.getState())) {
            address.setState(state);
            updated = true;
        }

        if(country != null && !country.equals(address.getCountry())) {
            address.setCountry(country);
            updated = true;
        }

        if(postalCode != null && !postalCode.equals(address.getPostalCode())) {
            address.setPostalCode(postalCode);
            updated = true;
        }

        if(updated) {
            addressRepository.persist(address);
            return Response.accepted().build();
        }

        return Response.notModified().build();

    }

    @DELETE
    @Path("/{contact_id}")
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response deleteContact(@PathParam("contact_id") Long contactId) {
        Contact contact = repository.findById(contactId);

        if (contact == null) {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Contact").build();
        }

        repository.delete(contact);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{contact_id}/address/{address_id}")
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response deleteContactAddress(@PathParam("contact_id") Long contactId, @PathParam("address_id") Long addressId) {
        Contact contact = repository.findById(contactId);

        if(contact == null) {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
        }

        Address contactAddress = addressRepository.find("select a from Address a where a.contactId = ?1", contactId).firstResult();

        if(contactAddress != null && contactAddress.getContact().getContactId().equals(contactId)) {
            log.debug("Found address {} for contact{}", addressId, contactId);
            addressRepository.delete(contactAddress);
            return Response.accepted().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Address").build();
        }
    }
}
