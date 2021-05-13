create table contacts (
    contact_id bigint generated always as identity not null,
    contact_first_name varchar(80),
    contact_last_name varchar(80) not null,
    contact_email_address varchar(200)
);

alter table contacts add constraint pk_contact_id primary key (contact_id);

create table address (
    address_id bigint generated always as identity not null,
    contact_id bigint not null,
    street_1 varchar(200),
    street_2 varchar(80),
    city varchar(80),
    state varchar(80),
    country varchar(80),
    postal_code varchar(12)
);

alter table address add constraint pk_address_id  primary key(address_id);
alter table address add constraint fk_address_client_id foreign key (contact_id) references contacts(contact_id);

/*
create table tax_claims (
    claim_id bigint generated always as identity not null,
    contact_id bigint not null,
    claim_year integer
);

alter table tax_claims add constraint pk_tax_claims primary key (claim_id);
alter table tax_claims add constraint fk_claim_contact_id foreign key (contact_id) references contacts(contact_id);

create table tax_claim_items (
    item_id bigint generated always as identity not null,
    claim_id bigint not null,
    item_name varchar(120),
    item_category_id integer,
    item_amount decimal(15,2)
);
alter table tax_claim_items add constraint pk_tax_claim_id primary key (item_id);
alter table tax_claim_items add constraint fk_claim_tax_claim_id foreign key (claim_id) references tax_claims(claim_id);

*/
