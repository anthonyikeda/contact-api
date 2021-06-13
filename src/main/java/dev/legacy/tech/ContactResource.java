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


@Path("/api/contact")
public class ContactResource {

    private final Logger log = LoggerFactory.getLogger(ContactResource.class);

    @Inject
    MeterRegistry registry;

    @Inject
    ContactService contactService;


    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContactCount() {
        long total = this.contactService.getContactCount();
        return Response.ok().header("X-Total-Contacts", total).build();
    }

    @Path("/{contactId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContactById(@PathParam("contactId") Long contactId) {
        try {
            ContactDTO contact = this.contactService.getContactById(contactId);
            return Response.ok(contact).build();
        } catch(ContactNotFoundException e) {
            log.error("Error getting contact with id {}", contactId, e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findContactsPaginated(@DefaultValue("0") @QueryParam("start") Integer start,
                                       @DefaultValue("10") @QueryParam("size") Integer size,
                                          @HeaderParam("Authorization") String authHeader) {
        log.info("Auth header: {}", authHeader);
        Timer.Sample contactRequest = Timer.start();
        log.debug("Querying for all contacts with start {} and size {}", start, size);
        List<ContactDTO> results = this.contactService.getPaginatedContacts(start, size);
        contactRequest.stop(registry.timer("contacts.rest.all.request"));
        return Response.ok(results).build();
    }

    @PUT
    @Path("/{contact_id}")
    @Consumes(value={"application/json"})
    @Produces(value={"application/json"})
    @Transactional
    public Response updateContact(@PathParam("contact_id") Long contactId, @QueryParam("first_name") String firstName, @QueryParam("last_name") String lastname,
                                  @QueryParam("email_address") String emailAddress) {
        try {
            ContactDTO contact = contactService.getContactById(contactId);

            boolean updated = false;

            if (firstName != null && !firstName.equals(contact.getFirstName())) {
                log.debug("Updating firstname: {}", firstName);
                contact.setFirstName(firstName);
                updated = true;
            }

            if (lastname != null && !lastname.equals(contact.getLastName())) {
                log.debug("Updating lastname: {}", lastname);
                contact.setLastName(lastname);
                updated = true;
            }

            if (emailAddress != null && !emailAddress.equals(contact.getEmailAddr())) {
                log.debug("Updating emailAddress: {}", emailAddress);
                contact.setEmailAddr(emailAddress);
                updated = true;
            }

            if (updated) {
                log.debug("Saving changed Contact {}", contactId);
                contactService.updateContact(contact);
                return Response.accepted().build();
            } else {
                return Response.status(Response.Status.NOT_MODIFIED).build();
            }
        } catch(ContactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
        }
    }

    @POST
    @Path("")
    @Transactional
    @Consumes(value = {"application/json", "application/x-www-form-urlencoded"})
    @Produces(value = {"application/json"})
    public Response createContact(@QueryParam("first_name") String firstName, @QueryParam("last_name") String lastname,
                                  @QueryParam("email_address") String emailAddress) {
        if(firstName == null || lastname == null || emailAddress == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Long contactId = contactService.createContact(firstName, lastname, emailAddress);
        log.debug("Saved contact, new id is: {}", contactId);
        return Response.created(URI.create(String.format("/api/contact/%d", contactId))).build();
    }

    @POST
    @Path("/{contact_id}/address")
    @Transactional
    @Consumes("application/json")
    @Produces(value = {"application/json"})
    public Response createContactAddress(@PathParam("contact_id") Long contactId, AddressDTO addressDTO) {

        try {
            ContactDTO contact = contactService.getContactById(contactId);

            List<AddressDTO> addresses = contactService.getContactAddresses(contact.getContactId());
            if (addresses.size() == 0) {
                AddressDTO address = new AddressDTO();
                address.setStreet1(addressDTO.getStreet1());
                address.setStreet2(addressDTO.getStreet2());
                address.setCity(addressDTO.getCity());
                address.setState(addressDTO.getState());
                address.setCountry(addressDTO.getCountry());
                address.setPostalCode(addressDTO.getPostalCode());

                Long addressId = contactService.createContactAddress(contact.getContactId(), address);
                address.setAddressId(addressId);
                log.debug("Saved address {} for contact {}", address.getAddressId(), contact.getContactId());
                return Response.created(URI.create(String.format("/api/contact/%d/address/%d", contact.getContactId(), address.getAddressId()))).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(String.format("Contact already has an address with id %d", addresses.get(0).getAddressId())).build();
            }
        } catch(ContactNotFoundException cnfe) {
            log.error("Unable to find contact {}", contactId, cnfe);
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
        } catch(ContactAddressAlreadyExistsException caee) {
            log.error("Contact already has an address", caee);
            return Response.status(Response.Status.BAD_REQUEST).entity(caee.getMessage()).build();
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
        try {
            ContactDTO contact = contactService.getContactById(contactId);

            if (contact == null) {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Contact").build();
            }

            if (contact.getAddress() == null) {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Address").build();
            }

            AddressDTO address = contactService.getContactAddressById(addressId);

            boolean updated = false;

            if (street1 != null && !street1.equals(address.getStreet1())) {
                address.setStreet1(street1);
                updated = true;
            }

            if (street2 != null && !street2.equals(address.getStreet2())) {
                address.setStreet2(street2);
                updated = true;
            }

            if (city != null && !city.equals(address.getCity())) {
                address.setCity(city);
                updated = true;
            }

            if (state != null && !state.equals(address.getState())) {
                address.setState(state);
                updated = true;
            }

            if (country != null && !country.equals(address.getCountry())) {
                address.setCountry(country);
                updated = true;
            }

            if (postalCode != null && !postalCode.equals(address.getPostalCode())) {
                address.setPostalCode(postalCode);
                updated = true;
            }

            if (updated) {
                try {
                    contactService.updateAddress(address);
                    return Response.accepted().build();
                } catch(AddressNotFoundException anfe) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            }
        } catch(Exception e) {
            log.error("Error finding processing address with id {}", addressId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.notModified().build();

    }

    @DELETE
    @Path("/{contact_id}")
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response deleteContact(@PathParam("contact_id") Long contactId) {
        try {
            ContactDTO contact = contactService.getContactById(contactId);
            contactService.deleteContactById(contact.getContactId());
            return Response.accepted().build();
        } catch(ContactNotFoundException e) {
            log.error("Error deleting contact with id {}", contactId, e);
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Contact").build();
        }
    }

    @DELETE
    @Path("/{contact_id}/address/{address_id}")
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response deleteContactAddress(@PathParam("contact_id") Long contactId, @PathParam("address_id") Long addressId) {
        try {
            ContactDTO contact = contactService.getContactById(contactId);

            AddressDTO contactAddress = contact.getAddress();

            if (contactAddress != null && contactAddress.getAddressId().equals(addressId)) {
                log.debug("Found address {} for contact {}", addressId, contactId);
                contactService.deleteAddressById(contactAddress.getAddressId());
                return Response.accepted().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Address").build();
            }
        } catch(ContactNotFoundException e) {
            log.error("Error deleting address with id {} for contact {}", addressId, contactId, e);
            return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
        }
    }
}
