library(httr)
library(tidyverse)
url = "http://localhost:8080/api/disparate_impact"
body = '{"prediction":"risk", "table":"test_data2", "protected_column":"sex", "scoring_timestamp":"timestamp"}'
res = POST(url = url, content_type_json(), body = body)
data = content(res)
library(purrr)

df = tibble(prediction = lapply(data, `[[`, 1), sex = lapply(data, `[[`, 2 ),group = lapply(data, `[[`, 3 ),disparate_impact = lapply(data, `[[`, 4 ),time = lapply(data, `[[`, 5 ))
df = as_tibble(lapply(df, unlist))%>% mutate(time = as.POSIXct(time))
df %>% ggplot(aes(x = time, group = interaction(sex, prediction), colour = interaction(sex,prediction), y = disparate_impact)) + geom_line() + xlab("Time") +  geom_point( size=4, shape=21, fill="white")

library(httr)
library(tidyverse)
url = "http://localhost:8080/api/explainability"
body = '{"table_name":"scored_credit", "target":"prediction", "features":["loanduration"], "id_column":"scoring_id", "max_iter":"10000","learn_rate":".002", "ids":[15005, 15006] }'
res = POST(url = url, content_type_json(), body = body)
data = lapply(content(res), `[[`, 1)

df = tibble(scoring_id = lapply(data, `[[`, 1), b1 = lapply(data, `[[`, 2 ),mse = lapply(data, `[[`, 3 ),iteration = lapply(data, `[[`, 4 ),loanduration = lapply(data, `[[`, 5 ))
df = as_tibble(lapply(df, unlist))%>%mutate(prediction = case_when(round(b1*loanduration)==0 ~"No Risk",round(b1*loanduration)==1 ~"Risk" ))

df %>% ggplot(aes(x ="LOANDURATION", y = b1*loanduration, fill= factor(prediction))) + geom_bar(position = "dodge", stat= "identity") + coord_flip() + ggtitle("Contribution of Loan Duration to Prediction of Risk")
