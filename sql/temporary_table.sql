create  bufferpool test pagesize 32k
create  user tablespace test  pagesize 32k bufferpool test
declare global temporary table test1(col1 int) in test
update dbm cfg using intra_parallel yes
declare global temporary table test1 as (select 1 as col1 from sysibm.sysdummy1) with data in test
declare mycurs cursor for select col1 from session.test1
insert into test (select * from session.test1)
declare mycurs cursor for select col1 from session.test1
load from mycurs of cursor insert into test




CREATE SEQUENCE transactions_seq
     START WITH 1
     INCREMENT BY 1
     NO MAXVALUE
     NO CYCLE
     CACHE 24;

CREATE SEQUENCE transactions_mqt_seq
     START WITH 0
     INCREMENT BY 1
     NO MAXVALUE
     NO CYCLE
     CACHE 24;

create or replace function split( string varchar(3200))
returns table(string VARCHAR(3200), rowid int)
return with initial(string, location, remainder) as
       (select
       case when locate(',', string) = 0 then string else SUBSTR(string, 1, locate(',', string)-1) end as string ,
       locate(',', string) as location,
       case when locate(',', string) = 0 then string else SUBSTR(string, locate(',', string)+1) end as remainder
        from sysibm.sysdummy1
       union all
       select
        case when locate(',', remainder) = 1 then LTRIM(REMAINDER, ',') else SUBSTR(remainder, 1, locate(',', remainder)-1) end  as string ,
       locate(',', remainder) as location,
       SUBSTR(remainder, locate(',', remainder)) as remainder
       from  initial
       where location >1)
       select string, row_number() over() as rowid from initial

CREATE TABLE transactions (
        model_id    INT NOT NULL DEFAULT 1,
        transaction_id int not null ,
        feature_name    varchar(36) not null,
        feature_value    FLOAT not null,
      scoring_timestamp TIMESTAMP not null with default current_timestamp,
        scoring_date  GENERATED ALWAYS AS (DATE(scoring_timestamp)),
        primary key(transaction_id, feature_name))
        --APPEND YES;
         --- organize by column



create or replace view transactions_v
as
select
transaction_id,
listagg(feature_name, ',') as feature_names,
listagg(feature_value, ',') as feature_values
from transactions
group by transaction_id;


CREATE or REPLACE TRIGGER transactions_insert INSTEAD OF INSERT ON transactions_v
REFERENCING NEW AS tr_row
FOR EACH row MODE DB2SQL
BEGIN
insert into transactions(transaction_id, feature_name, feature_value) with next(next) as
  (select
  next value for transactions_seq
  from sysibm.sysdummy1)
  select
  n.next as transaction_id,
  t.feature_name,
  t.feature_value
  from next n
  join
  (
  select
  t1.string as feature_name,
  t2.string as feature_value
  from table(split(tr_row.feature_names)) t1
  join
  table(split(tr_row.feature_values))
  t2
  on t1.rowid = t2.rowid
  )
    t on 1=1;
    call refreshTransactions();
END


 CREATE TABLE transactions_mqt AS
      (SELECT
      model_id,
      transaction_id,
      feature_name,
      feature_value,
      scoring_timestamp
        FROM transactions
      )
      DATA INITIALLY DEFERRED
      REFRESH DEFERRED
      MAINTAINED BY REPLICATION
      ORGANIZE BY COLUMN;

SET INTEGRITY FOR TRANSACTIONS_MQT ALL IMMEDIATE UNCHECKED


ALTER TABLE TRANSACTIONS_MQT
    ADD CONSTRAINT TRANSACTIONS_MQT_IDX PRIMARY KEY (TRANSACTION_ID, FEATURE_NAME);

select * from transactions where transaction_id not in  (select transaction_id from transactions_mqt)



create or replace procedure refreshTransactions()
begin
DECLARE STM1 VARCHAR(255);
DECLARE T_1D INT;
SET T_1D = (select max(transaction_id) from transactions_mqt);
SET STM1 =  'SET INTEGRITY FOR TRANSACTIONS_MQT ALL IMMEDIATE UNCHECKED';

INSERT INTO transactions_mqt(model_id,transaction_id, feature_name, feature_value, scoring_timestamp) (select
   t1.model_id, t1.transaction_id,     t1.feature_name,  t1.feature_value,
  t1.scoring_timestamp from transactions t1 where transaction_id > T_1D );

   --EXECUTE IMMEDIATE STM1;
end



CREATE TYPE FEATURE_VALUES AS FLOAT ARRAY[];
CREATE TYPE FEATURE_NAMES AS VARCHAR(36) ARRAY[];

create or replace procedure processTransactions()
 begin
 declare feature_values FEATURE_VALUES;
 declare feature_names FEATURE_NAMES;
 declare transaction_id int;
 set transaction_id = next value for transactions_seq;

 set feature_values = ARRAY[0.5,0.6,0.7];
 set feature_names = ARRAY['Bob', 'Ann', 'Sue'];

insert into transactions(transaction_id, feature_name, feature_value)
 (select transaction_id, T.n, T.v from UNNEST(feature_names, feature_values) as T(n, v));

end








