drop table if exists scheduler_events CASCADE;
drop table if exists appointments CASCADE;
drop table if exists events_type CASCADE;


CREATE TABLE IF NOT EXISTS events_type(

  id uuid,
  name varchar,
  color varchar,
  description varchar
);

ALTER TABLE events_type  ADD CONSTRAINT pk_events_type  PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS appointments(

   id uuid,
   events_type uuid,
   "user" uuid,
   initial_date timestamp,
   final_date timestamp,
   local varchar
);

ALTER TABLE appointments  ADD CONSTRAINT pk_appointments  PRIMARY KEY (id);
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_events_type_events_type FOREIGN KEY (events_type) REFERENCES events_type(id);
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_user_configuration_user FOREIGN KEY ("user") REFERENCES user_configuration(id);


