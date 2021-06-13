package dev.legacy.tech;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ContactRepository implements PanacheRepository<ContactDAO> {

    public List<ContactDAO> findByCity(String city) {
        return find("address.city", city).list();
    }
}
