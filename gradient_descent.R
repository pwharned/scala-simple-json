library(tidyverse)
data = iris %>% as_tibble()%>% mutate(target = case_when(Species == "setosa" ~ 0, Species == "versicolor" ~1, Species=="virginica" ~2))%>%arrange(Sepal.Length)
regression <- function(x, y, learn_rate, n, max_iter) {
  plot(x, y, col = "blue", pch = 20)
  m <- runif(1, 0, 1)
  c <- runif(1, 0, 1)
  yhat <- m * x + c
  MSE <- sum((y - yhat) ^ 2) / n
  converged = F
  iterations = 0
  while(iterations < max_iter) {
    ## Implement the gradient descent algorithm
    m <- m - learn_rate * ((1 / n) * (sum((yhat - y) * x)))
    c <- c - learn_rate * ((1 / n) * (sum(yhat - y)))
    yhat <- m * x + c
    MSE <- sum((y - yhat) ^ 2) / n
    iterations = iterations + 1

  }
  abline(c, m) 
  
  return(paste("intercept:", c, "slope:", m, "MSE", MSE))
  
}
# Run the function 

regression( data$Sepal.Length,data$target, learn_rate = 0.0025,n =  150, max_iter =  50000)


