# BayesianNetworkQuery
A command line program for constructing Bayesian networds and doing queries on them.

For a small demonstration, run the JAR file and copy to the console

    node B 0.9  # Battery
    node R 0.9  # Radio
    node I 0.95 # Ignition
    node F 0.95 # Fuel 
    node S 0.99 # Starts
    node M 0.99 # Moves
    connect B to R
    connect B to I
    connect I to S
    connect F to S
    connect S to M
    print
    p(not M | not B, R)
    p(M | not B, R)

Above, `p(not M | not B, R)` means "What is the probability of `M` not working if we know that `B` does not work and `R` does?
