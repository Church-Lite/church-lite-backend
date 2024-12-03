CREATE TABLE IF NOT EXISTS scheduler_events(

   id uuid,
   scheduler_type uuid,
   "user" uuid,
   initial_date timestamp,
   final_date timestamp,
   local varchar
);

ALTER TABLE scheduler_events  ADD CONSTRAINT pk_scheduler_events  PRIMARY KEY (id);
ALTER TABLE scheduler_events ADD CONSTRAINT fk_scheduler_events_scheduler_type_scheduler_type FOREIGN KEY (scheduler_type) REFERENCES scheduler_type(id);
ALTER TABLE scheduler_events ADD CONSTRAINT fk_scheduler_events_user_configuration_user FOREIGN KEY ("user") REFERENCES user_configuration(id);


alter table scheduler_type add description varchar;