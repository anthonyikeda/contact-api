package dev.legacy.tech;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ContactRepository implements PanacheRepository<Contact> {

    private final Logger log = LoggerFactory.getLogger(ContactRepository.class);

    public List<Contact> searchByName(String firstName, String lastName) {
        return list("select contact from Contact as contact where contact.firstName=?1 and contact.lastName=?2",
                firstName, lastName);
    }

    public List<Contact> searchByCity(String city, Integer start, Integer size) {
        log.debug("querying for contacts living in {}", city);
        return find("select contact from Contact contact join fetch contact.address where contact.address.city = ?1", city)
                .page(start, size)
                .list();
    }
}
