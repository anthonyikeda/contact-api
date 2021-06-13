package dev.legacy.tech;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContactService {

    private final Logger log = LoggerFactory.getLogger(ContactService.class);

    @Inject
    ContactRepository contactRepository;

    @Inject
    AddressRepository addressRepository;

    @Inject
    MeterRegistry registry;

    @Transactional
    public Long createContact(String firstName, String lastName, String emailAddress) {
        log.debug("Creating new contact {} {}", firstName, lastName);

        ContactDAO contact = new ContactDAO();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setEmailAddress(emailAddress);

        this.contactRepository.persist(contact);

        log.debug("Saved contact. New id is {}", contact.getContactId());

        return contact.getContactId();
    }

    public void updateContact(ContactDTO contactDTO) {

        ContactDAO found = contactRepository.findById(contactDTO.getContactId());

        found.setFirstName(contactDTO.getFirstName());
        found.setLastName(contactDTO.getLastName());
        found.setEmailAddress(contactDTO.getEmailAddr());

        contactRepository.persist(found);
        log.debug("Updated contact with id {}", found.getContactId());
    }

    public Long getContactCount() {
        return this.contactRepository.count();
    }

    public ContactDTO getContactById(Long contactId) throws ContactNotFoundException {
        try {
            ContactDAO dao = contactRepository.findById(contactId);
            return convertDAOtoDTO(dao);
        } catch(NullPointerException npe) {
            throw new ContactNotFoundException(String.format("Contact with id %d was not found", contactId));
        }
    }

    private ContactDTO convertDAOtoDTO(ContactDAO dao) {
        Timer.Sample readContactTimer = Timer.start();
        ContactDTO dto = new ContactDTO();
        dto.setContactId(dao.getContactId());
        dto.setFirstName(dao.getFirstName());
        dto.setLastName(dao.getLastName());
        dto.setEmailAddr(dao.getEmailAddress());

        if(dao.getAddress() != null) {
            dto.setAddress(convertAddressDAOtoDTO(dao.getAddress()));
        }
        readContactTimer.stop(registry.timer("contact.dao.convert"));
        return dto;
    }

    private AddressDTO convertAddressDAOtoDTO(AddressDAO dao) {
        Timer.Sample addressTimer = Timer.start();
        AddressDTO address = new AddressDTO();
        address.setAddressId(dao.getAddressId());
        address.setStreet1(dao.getStreet1());
        address.setStreet2(dao.getStreet2());
        address.setCity(dao.getCity());
        address.setCountry(dao.getCountry());
        address.setPostalCode(dao.getPostalCode());
        addressTimer.stop(registry.timer("address.dao.convert"));
        return address;
    }

    public List<ContactDTO> getPaginatedContacts(Integer start, Integer size) {
        log.debug("Querying database for all contacts with start {} and size {}", start, size);
        Timer.Sample queryTimer = Timer.start(registry);

        List<ContactDTO> results = this.contactRepository.findAll().page(start, size)
                .stream()
                .map(this::convertDAOtoDTO)
                .collect(Collectors.toList());

        queryTimer.stop(registry.timer("contacts.all.jdbc.query"));

        return results;
    }

    @Transactional
    public Long createContactAddress(Long contactId, AddressDTO address) throws ContactAddressAlreadyExistsException {
        ContactDAO contact = contactRepository.findById(contactId);

        if(contact.getAddress() == null) {
            AddressDAO addressDAO = new AddressDAO();
            addressDAO.setStreet1(address.getStreet1());
            addressDAO.setStreet2(address.getStreet2());
            addressDAO.setCity(address.getCity());
            addressDAO.setState(address.getState());
            addressDAO.setCountry(address.getCountry());
            addressDAO.setPostalCode(address.getPostalCode());

            addressDAO.setContact(contact);
            addressRepository.persist(addressDAO);
            return addressDAO.getAddressId();
        } else {
            throw new ContactAddressAlreadyExistsException(String.format("Customer already has an address with id %d", contact.getAddress().getAddressId()));
        }
    }

    public List<AddressDTO> getContactAddresses(Long contactId) {
        log.debug("Retrieving contact addresses with id {}", contactId);

        return this.addressRepository.findByContactId(contactId)
                .stream()
                .map(this::convertAddressDAOtoDTO)
                .collect(Collectors.toList());
    }

    public AddressDTO getContactAddressById(Long addressId) {
        Timer.Sample timer = Timer.start();
        AddressDAO address = this.addressRepository.findById(addressId);
        AddressDTO dto = convertAddressDAOtoDTO(address);
        timer.stop(registry.timer("address.by.id"));
        return dto;
    }

    public void updateAddress(AddressDTO address) throws AddressNotFoundException {

        AddressDAO dao = this.addressRepository.findById(address.getAddressId());
        if(dao != null) {
            Timer.Sample timer = Timer.start();
            dao.setStreet1(address.getStreet1());
            dao.setStreet2(address.getStreet2());
            dao.setCity(address.getCity());
            dao.setState(address.getState());
            dao.setPostalCode(address.getPostalCode());
            dao.setCountry(address.getCountry());

            this.addressRepository.persist(dao);
            timer.stop(this.registry.timer("address.convert.update"));
            log.debug("Updated addressId {}", address.getAddressId());
        } else {
            throw new AddressNotFoundException(String.format("Unable to locate address with id %d", address.getAddressId()));
        }

    }

    @Transactional
    public void deleteContactById(Long contactId) {

        //First delete their addresses if they exist
        List<AddressDTO> addresses = getContactAddresses(contactId);

        addresses.stream().map(AddressDTO::getAddressId).forEach(this::deleteAddressById);

        this.contactRepository.deleteById(contactId);
        log.info("Deleted contact with id {}", contactId);

    }

    @Transactional
    public void deleteAddressById(Long addressId) {
        this.addressRepository.deleteById(addressId);
    }
}
