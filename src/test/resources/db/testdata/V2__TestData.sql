insert into contacts (contact_id, contact_first_name, contact_last_name, contact_email_address) OVERRIDING SYSTEM VALUE values(1, 'Michael', 'Masters', 'mmasters@home.com');
insert into contacts (contact_id, contact_first_name, contact_last_name, contact_email_address) OVERRIDING SYSTEM VALUE values(2, 'Andrea', 'Aminos', 'aaminos@home.com');
insert into contacts (contact_id, contact_first_name, contact_last_name, contact_email_address) OVERRIDING SYSTEM VALUE values(3, 'Sarah', 'Jackson', 'sjackson@home.com');

insert into address (address_id, contact_id, street_1, street_2, city, state, country, postal_code) OVERRIDING SYSTEM VALUE values(12, 1, '2392 Old Gate Place', '2210', 'Sacramento', 'California', 'United States', '94291');
insert into address (address_id, contact_id, street_1, street_2, city, state, country, postal_code) OVERRIDING SYSTEM VALUE values(13, 2, '6253 8th Court', 'Apt 54', 'Sacramento', 'California', 'United States', '94230');
insert into address (address_id, contact_id, street_1, street_2, city, state, country, postal_code) OVERRIDING SYSTEM VALUE values(14, 3, '0539 Straubel Drive', 'Unit 2B', 'Sacramento', 'California', 'United States', '95852');

SELECT setval('contacts_contact_id_seq', (SELECT MAX(contact_id) from "contacts"));
SELECT setval('address_address_id_seq', (SELECT MAX(address_id) from "address"));

