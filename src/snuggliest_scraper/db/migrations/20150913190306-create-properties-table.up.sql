CREATE TABLE IF NOT EXISTS property (
  pk_property_id serial PRIMARY KEY,
  scraped_at timestamp NOT NULL,
  source varchar(25) NOT NULL,
  code varchar(25) NOT NULL,
  status varchar(25) NOT NULL,
  url varchar(255) NOT NULL,
  title varchar(255),
  ppw integer,
  ppm integer,
  latitude varchar(25),
  longitude varchar(25),
  address varchar(255),
  street varchar(50),
  postal_code varchar(25),
  UNIQUE(source, code)
)
