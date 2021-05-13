COPY contacts(contact_id, contact_first_name, contact_last_name, contact_email_address)
FROM '/Users/anthonyikeda/work/git_mvp/finance-monolith-app/src/main/resources/db/migration/contacts.csv'
DELIMITER ','
CSV HEADER ;

