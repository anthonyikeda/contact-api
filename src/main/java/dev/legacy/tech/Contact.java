package dev.legacy.tech;

import javax.persistence.*;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="contact_id")
    private Long contactId;

    @Column(name="contact_first_name")
    private String firstName;

    @Column(name="contact_last_name")
    private String lastName;

    @Column(name="contact_email_address")
    private String emailAddr;

    @OneToOne(mappedBy = "contact", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
    @JoinColumn(name="contact_id")
    private Address address;

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

    public String getEmailAddr() {
        return emailAddr;
    }

    public void setEmailAddr(String emailAddr) {
        this.emailAddr = emailAddr;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
