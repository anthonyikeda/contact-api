package dev.legacy.tech;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ContactService {

    private final Logger log = LoggerFactory.getLogger(ContactService.class);

    @Inject
    MeterRegistry registry;

    @Inject
    DataSource dataSource;

    public List<ContactDTO> getPaginatedContacts(Integer start, Integer size) throws SQLException {
        log.debug("Querying database for all contacts with start {} and size {}", start, size);

        List<ContactDTO> results = new ArrayList<>(size);
        String query = "SELECT * from contacts c join address a on a.contact_id = c.contact_id limit ? offset ?";

        Timer.Sample queryTimer = Timer.start(registry);
        try(Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, start);
            pstmt.setInt(2, size);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                ContactDTO contact = new ContactDTO();
                contact.setContactId(rs.getLong("contacts.contact_id"));
                contact.setFirstName(rs.getString("contact_first_name"));
                contact.setLastName(rs.getString("contact_last_name"));
                contact.setEmailAddr(rs.getString("contact_email_address"));

                AddressDTO address = new AddressDTO();
                address.setAddressId(rs.getLong("address_id"));
                address.setStreet1(rs.getString("street_1"));
                address.setStreet2(rs.getString("street_2"));
                address.setCity(rs.getString("city"));
                address.setCountry(rs.getString("country"));
                address.setPostalCode(rs.getString("postal_code"));

                contact.setAddress(address);
                results.add(contact);
            }

            try {
                rs.close();
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}
            queryTimer.stop(registry.timer("contacts.all.jdbc.query"));
        } catch(SQLException sqle) {
            log.error("Error connecting to the database", sqle);
            throw sqle;
        }
        return results;
    }
}
