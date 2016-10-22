# BayesianNetworkQuery
A command line program for constructing binary (each variable has two states: on and off) Bayesian networks and doing queries on them.

In order to run the demo, invoke the JAR file with `java -jar YOUR_JAR.jar demo.txt`

Even better, if you have both __Maven__ and __git__ client set up, you could try:

   `git clone git@github.com:coderodde/BayesianNetworkQuery.git && cd BayesianNetworkQuery && mvn compiler:compile && mvn jar:jar && java -jar target/BayesianNetworkQuery-1.618.jar demo_network.txt demo_queries.txt` 
