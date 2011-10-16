#!/bin/bash

## Classpath
CP=`lein classpath`

## List of languages or technologies for quering.
LANGS="yz hql"

## Number of queries.
N=9

## Name of the persistence unit.
name="bench"


## Derby settings.
url="jdbc:derby:db1;create=true"
dialect="org.hibernate.dialect.DerbyDialect"
driver="org.apache.derby.jdbc.EmbeddedDriver"
derby="$url $dialect $driver"

## H2 settings.
url_h2="jdbc:h2:db1/db1;create=true"
dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"
h2="$url_h2 $dialect_h2 $driver_h2"


## HSQLDB settings.
url_hsqldb="jdbc:hsqldb:db1/db1;create=true"
dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
driver_hsqldb="org.apache.derby.jdbc.EmbeddedDriver"
hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"

## Current settings.
db=$h2

## List with number of elements for DB.
counts="100 1000 10000 25000 50000 75000 100000 250000 500000 750000 1000000"

## Java options (Needed for big databases (since 250000 elements)).
JAVA_OPTS="-Xss128M -Xmx1G"

for c in $counts; do 
    echo $c
    # Generate the DB with specified amount of elements.
    java $JAVA_OPT -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bd-utils $c $name $db
    for i in `seq 0 $N`; do 
	res=""
	for lang in $LANGS; do
	    res+=`java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.benchmark $i $name $db $lang`
	    res+="    "
	done;
	echo $res
    done;
done;
