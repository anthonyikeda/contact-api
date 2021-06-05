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
        try {
            long total = this.contactService.getContactCount();
            return Response.ok().header("X-Total-Contacts", total).build();
        } catch(Exception e) {
            log.error("Error getting contact count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Path("/{contactId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContactById(@PathParam("contactId") Long contactId) {
        try {
            ContactDTO contact = this.contactService.getContactById(contactId);
            if(contact == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(contact).build();
        } catch(Exception e) {
            log.error("Error getting contact with id {}", contactId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
        try {
            List<ContactDTO> results = this.contactService.getPaginatedContacts(start, size);
            contactRequest.stop(registry.timer("contacts.rest.all.request"));
            return Response.ok(results).build();
        } catch(Exception e) {
            log.error("Error getting the page of results", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
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

            if (contact == null) {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
            }

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
            }
        } catch(Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.notModified().build();
    }

    @POST
    @Path("")
    @Transactional
    @Consumes(value = {"application/json", "application/x-www-form-urlencoded"})
    @Produces(value = {"application/json"})
    public Response createContact(@QueryParam("first_name") String firstName, @QueryParam("last_name") String lastname,
                                  @QueryParam("email_address") String emailAddress) {
        try {
            Long contactId = contactService.createContact(firstName, lastname, emailAddress);
            log.debug("Saved contact, new id is: {}", contactId);
            return Response.created(URI.create(String.format("/api/contact/%d", contactId))).build();
        } catch(Exception e) {
            log.error("Error creating new contact", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/{contact_id}/address")
    @Transactional
    @Consumes("application/json")
    @Produces(value = {"application/json"})
    public Response createContactAddress(@PathParam("contact_id") Long contactId, AddressDTO addressDTO) {

        try {
            ContactDTO contact = contactService.getContactById(contactId);

            if (contact == null) {
                log.debug("Contact {} does not exist", contactId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<AddressDTO> addresses = contactService.getContactAddresses(contactId);
            if(addresses.size() == 0) {
                AddressDTO address = new AddressDTO();
                address.setStreet1(addressDTO.getStreet1());
                address.setStreet2(addressDTO.getStreet2());
                address.setCity(addressDTO.getCity());
                address.setState(addressDTO.getState());
                address.setCountry(addressDTO.getCountry());
                address.setPostalCode(addressDTO.getPostalCode());

                Long addressId = contactService.createContactAddress(contactId, address);
                address.setAddressId(addressId);
                log.debug("Saved address {} for contact {}", address.getAddressId(), contactId);
                return Response.created(URI.create(String.format("/api/contact/%d/address%d", contactId, address.getAddressId()))).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(String.format("Contact already has an address with id %d", addresses.get(0).getAddressId())).build();
            }

        } catch(Exception e) {
            log.error("Error finding contact with id {}", contactId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
                contactService.updateAddress(address);
                return Response.accepted().build();
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

            if (contact == null) {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Contact").build();
            }

            contactService.deleteContactById(contact.getContactId());
            return Response.accepted().build();
        } catch(Exception e) {
            log.error("Error deleting contact with id {}", contactId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{contact_id}/address/{address_id}")
    @Consumes(value = {"application/json"})
    @Produces(value = {"application/json"})
    public Response deleteContactAddress(@PathParam("contact_id") Long contactId, @PathParam("address_id") Long addressId) {
        try {
            ContactDTO contact = contactService.getContactById(contactId);

            if (contact == null) {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Client").build();
            }

            AddressDTO contactAddress = contactService.getContactAddresses(contactId).get(0);

            if (contactAddress != null) {
                log.debug("Found address {} for contact{}", addressId, contactId);
                contactService.deleteAddressById(contactAddress.getAddressId());
                return Response.accepted().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).header("X-Not-Found", "Address").build();
            }
        } catch(Exception e) {
            log.error("Error deleting address with id {}", addressId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
