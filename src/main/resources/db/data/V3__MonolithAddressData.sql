COPY address(address_id, contact_id, street_1, street_2, city, state, postal_code)
    FROM '/Users/anthonyikeda/work/git_mvp/finance-monolith-app/src/main/resources/db/data/addresses.csv'
    DELIMITER ','
    CSV HEADER ;
