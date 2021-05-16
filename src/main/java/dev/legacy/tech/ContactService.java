package dev.legacy.tech;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ContactService {

    private final Logger log = LoggerFactory.getLogger(ContactService.class);

    @Inject
    MeterRegistry registry;

    @Inject
    DataSource dataSource;

    public Long createContact(String firstName, String lastName, String emailAddress) throws SQLException {
        Long contactId = 0L;

        StringBuilder insert = new StringBuilder("insert into contact (contact_first_name, contact_last_name, contact_email_address)");
        insert.append(" values(?, ?, ?)");

        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(insert.toString(), Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, emailAddress);
            boolean executed = pstmt.execute();

            if(executed) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if(rs.next()) {
                    contactId = rs.getLong(1);
                }
                rs.close();
            }

            try {
                pstmt.close();
                con.close();
            } catch(SQLException sql) {}

            return contactId;
        }
    }

    public void updateContact(ContactDTO contact) throws SQLException {
        StringBuilder update = new StringBuilder("update contacts set contact_first_name = ? ");
            update.append("contact_last_name = ?");
            update.append("contact_email_address = ?");
            update.append("where contact_id = ?");

        PreparedStatement pstmt = null;

        try (Connection con = dataSource.getConnection()) {
            pstmt = con.prepareStatement(update.toString());
            pstmt.setString(1, contact.getFirstName());
            pstmt.setString(2, contact.getLastName());
            pstmt.setString(3, contact.getEmailAddr());
            pstmt.setLong(4, contact.getContactId());

            int updated = pstmt.executeUpdate();

            if(updated > 0) {
                log.debug("Updated contact with id {}", contact.getContactId());
            }

            try {
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}
        }
    }

    public Long getContactCount() throws SQLException {
        String query = "select count(*) from contacts";
        long totalResults = 0L;

        try(Connection con = dataSource.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if(rs.next()) {
                totalResults = rs.getLong(1);
            }
            try {
                rs.close();
                stmt.close();
                con.close();
            } catch(SQLException sqle) {}

            return totalResults;
        }
    }
    public ContactDTO getContactById(Long contactId) throws SQLException {
        String select = "select * from contacts c join address a on a.contact_id = c.contact_id where c.contact_id = ?";
        ContactDTO contact = null;

        try(Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(select);
            pstmt.setLong(1, contactId);

            ResultSet rs = pstmt.executeQuery();

            if(rs.next()) {
                contact = contactFromResultSet(rs);
            }

            try {
                rs.close();
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}

            return contact;
        }

    }

    private AddressDTO addressFromResultSet(ResultSet rs) throws SQLException {
        AddressDTO address = new AddressDTO();
        address.setAddressId(rs.getLong("address_id"));
        address.setContactId(rs.getLong("contact_id"));
        address.setStreet1(rs.getString("street_1"));
        address.setStreet2(rs.getString("street_2"));
        address.setCity(rs.getString("city"));
        address.setCountry(rs.getString("country"));
        address.setPostalCode(rs.getString("postal_code"));
        return address;
    }

    private ContactDTO contactFromResultSet(ResultSet rs) throws SQLException {
        ContactDTO contact = new ContactDTO();
        contact.setContactId(rs.getLong("contact_id"));
        contact.setFirstName(rs.getString("contact_first_name"));
        contact.setLastName(rs.getString("contact_last_name"));
        contact.setEmailAddr(rs.getString("contact_email_address"));


        contact.setAddress(addressFromResultSet(rs));
        return contact;
    }

    public List<ContactDTO> getPaginatedContacts(Integer start, Integer size) throws SQLException {
        log.debug("Querying database for all contacts with start {} and size {}", start, size);

        List<ContactDTO> results = new ArrayList<>(size);
        String query = "SELECT * from contacts c join address a on a.contact_id = c.contact_id limit ? offset ?";

        Timer.Sample queryTimer = Timer.start(registry);
        try(Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, size);
            pstmt.setInt(2, start);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                ContactDTO contact = contactFromResultSet(rs);
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

    public Long createContactAddress(Long contactId, AddressDTO address) throws SQLException {
        StringBuilder insert = new StringBuilder("insert into address (contact_id, street_1, street_2, city, state, country, postal_code) ");
        insert.append(" values(?, ?, ?, ?, ?, ?, ?)");

        try (Connection con = dataSource.getConnection()) {
            Long addressId = 0L;

            PreparedStatement pstmt = con.prepareStatement(insert.toString(), Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, contactId);
            pstmt.setString(2, address.getStreet1());
            pstmt.setString(3, address.getStreet2());
            pstmt.setString(4, address.getCity());
            pstmt.setString(5, address.getState());
            pstmt.setString(6, address.getCountry());
            pstmt.setString(7, address.getPostalCode());

            boolean executed = pstmt.execute();

            if(executed) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if(rs.next()) {
                    addressId = rs.getLong(1);
                }

                rs.close();
            }

            try {
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}

            return addressId;
        }
    }

    public List<AddressDTO> getContactAddresses(Long contactId) throws SQLException {
        List<AddressDTO> results = new ArrayList<>();
        String query = "select * from address where contact_id = ?";

        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setLong(1, contactId);

            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                AddressDTO address = addressFromResultSet(rs);
                results.add(address);
            }

            rs.close();

            try {
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}

            return results;
        }
    }

    public AddressDTO getContactAddressById(Long addressId) throws SQLException {
        String query = "select * from address where address_id = ?";

        try (Connection con = dataSource.getConnection()) {
            AddressDTO address = null;
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setLong(1, addressId);

            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                address = addressFromResultSet(rs);
            }

            try {
                rs.close();
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}
            return address;
        }
    }

    public void updateAddress(AddressDTO address) throws SQLException {
        StringBuilder update = new StringBuilder("update address set street_1=?, street_2=?, city=?, state=?, country=?, postal_code=?");
        update.append(" where address_id=?");

        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(update.toString());
            pstmt.setString(1, address.getStreet1());
            pstmt.setString(2, address.getStreet2());
            pstmt.setString(3, address.getCity());
            pstmt.setString(4, address.getState());
            pstmt.setString(5, address.getCountry());
            pstmt.setString(6, address.getPostalCode());
            pstmt.setLong(7, address.getAddressId());

            int updatedRows = pstmt.executeUpdate();

            log.debug("Updated {} addresses for addressId {}", updatedRows, address.getAddressId());

            try {
                pstmt.close();
                con.close();
            } catch(SQLException sqle) {}
        }
    }

    public void deleteContactById(Long contactId) throws SQLException {

        //First delete their addresses if they exist
        List<AddressDTO> addresses = getContactAddresses(contactId);

        addresses.stream().map(AddressDTO::getAddressId).forEach(addressId -> {
            try {
                deleteAddressById(addressId);
            } catch(Exception e){
                log.error("Error deleting address {}", addressId);
            }
        });

        String delete = "delete from contacts where contact_id = ?";

        try(Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(delete);
            pstmt.setLong(1, contactId);
            int deletedRows = pstmt.executeUpdate();

            if(deletedRows > 0) {
                log.info("Deleted contact with id {}", contactId);
            } else {
                log.info("Was not able to delete contact with id {}", contactId);
            }
        }
    }

    public void deleteAddressById(Long addressId) throws SQLException {
        String delete = "delete from address where address_id = ?";

        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement(delete);
            pstmt.setLong(1, addressId);
            int deletedRows = pstmt.executeUpdate();
            if(deletedRows > 0) {
                log.info("Deleted {} addresses", deletedRows);
            } else {
                log.info("Did not delete any addresses with id {}", addressId);
            }
        }
    }
}
