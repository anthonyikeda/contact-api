package dev.legacy.tech;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class AddressRepository implements PanacheRepository<AddressDAO> {

    public List<AddressDAO> findByContactId(Long contactId) {
        return find("contact_id", contactId).list();
    }
}
