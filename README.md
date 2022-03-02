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