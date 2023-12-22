CREATE TABLE category (id Int64, name Text, description Text, age_group Text, created TIMESTAMP, inserted Int64, primary key (id));

CREATE TABLE lego_set (id Int32, name Text, min_age Int32, max_age Int32, primary key (id));
CREATE TABLE handbuch (handbuch_id Int64, author Text, text Text, primary key(handbuch_id));
CREATE TABLE model (name Text, description TEXT, lego_set Int32, primary key (lego_set, name));
