alter table cash add status varchar;

update cash set status = '1' where status is null and type_cash = '1';