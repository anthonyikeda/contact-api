package dev.legacy.tech;

import javax.persistence.*;

@Entity
public class AddressDAO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="address_id")
    private Long addressId;

    @OneToOne(targetEntity = ContactDAO.class)
    @JoinColumn(name="contact_id", referencedColumnName = "contact_id")
    private ContactDAO contact;

    @Column(name="street_1")
    private String street1;

    @Column(name="street_2")
    private String street2;

    @Column(name="city")
    private String city;

    @Column(name="state")
    private String state;

    @Column(name="postal_code")
    private String postalCode;

    @Column(name="country")
    private String country;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public ContactDAO getContact() {
        return contact;
    }

    public void setContact(ContactDAO contact) {
        this.contact = contact;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
