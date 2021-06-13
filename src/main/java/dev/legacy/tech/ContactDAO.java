package dev.legacy.tech;

import javax.persistence.*;

@Entity
public class ContactDAO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="contact_id")
    private Long contactId;

    @Column(name="contact_first_name")
    private String firstName;

    @Column(name="contact_last_name")
    private String lastName;

    @Column(name="contact_email_address")
    private String emailAddress;

    @OneToOne(targetEntity = AddressDAO.class)
    @JoinColumn(name = "contact_id", referencedColumnName = "contact_id")
    private AddressDAO address;

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public AddressDAO getAddress() {
        return address;
    }

    public void setAddress(AddressDAO address) {
        this.address = address;
    }
}
