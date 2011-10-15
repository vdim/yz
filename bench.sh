#!/bin/bash

## Classpath
CP=`lein classpath`

## List of languages or thechologies for quering.
LANGS="yz hql"

## Number of queries.
N=7

## Name of the persistence unit.
name="bench"

## Url for schema export.
url="jdbc:derby:db1;create=true"
url_hsqldb="jdbc:hsqldb:db1/db1;create=true"
url_h2="jdbc:h2:db1/db1;create=true"

dialect="org.hibernate.dialect.DerbyDialect"
driver="org.apache.derby.jdbc.EmbeddedDriver"

dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"

## List with number of elements for DB.
counts="100 1000 10000 25000 50000 75000 100000 250000 500000 750000 1000000"

for c in $counts; do 
    echo $c
    java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bd-utils $c $name $url_h2 $dialect_h2 $driver_h2
    for i in `seq 0 $N`; do 
	res=""
	for lang in $LANGS; do
	    res+=`java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.$lang $i`
	    res+="    "
	done;
	echo $res
    done;
done;
