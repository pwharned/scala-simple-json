# Aletheutes

Scalable,stateless api for doing MLOPS/ In Databse monitoring providing realtime analytics for deployed ml models

Bias detection track differences in model outcomes across different members of protected classes

Explainability Build interpretable in database models to explain the model outcomes

Drift - use in database modeling to detect shifts in probability of features relative to model outcome

Data Drift - track changes in feature distribution over time. 

## Explainability 

The implementation will provide ad-hoc in-database interpretable regression and logistic regression models that can be used to explain the models decision locally. 
There will be both global surrogate models and local models, similar to LIME but with simplifications to account for runtime and scalability issues.

The explainability features allow end users to send a model transaction id and feature array to the API, which will in turn run a query that builds a local model to predict the black box models prediction and scores the model against the requested transaction limited to the feature set specified by the user. 

## Dev

```podman pod create --name=db2 -p 50000:50000```
```podman run --pod=db2 -itd --name mydb2 --privileged=true -e LICENSE=accept -e DB2INST1_PASSWORD=password -e DBNAME=bludb ibmcom/db2```


## Tables

## Models

We use a row oriented table  for storing the model 

```create table models(id integer not null generated always as identity, name varchar(30), model clob, primary key(id))```


```
CREATE TABLE transactions (
  model_id    INT NOT NULL,
  transaction_id int not null generated always as identity,
  feature_name    varchar(36),
  feature_value    FLOAT,
scoring_timestamp TIMESTAMP not null with default current_timestamp,
  scoring_date  GENERATED ALWAYS AS (DATE(scoring_timestamp)),
  primary key(transaction_id))
    organize by column
```

```
create index transactions_idx on transactions(scoring_timestamp)
```

     PARTITION BY RANGE(scoring_date)
    (STARTING ('1/1/2023') ENDING ('12/31/2033') EVERY 2 MONTH)
    DISTRIBUTE BY HASH(SCORING_DATE)
      
;
    ```