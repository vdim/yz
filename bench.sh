#!/bin/bash

## Classpath
CP=`lein classpath`

## List of languages or thechologies for quering.
LANGS="yz hql"

## Number of queries.
N=7

## Name of the persistence unit.
name="bench"

## Url for doing schema export.
url="jdbc:derby:db1;create=true"

## List with number of elements for DB.
counts="100 1000 10000 100000 500000 1000000 2000000"

for c in $counts; do 
    echo $c
    java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bd-utils $c $name $url
    for i in `seq 0 $N`; do 
	res=""
	for lang in $LANGS; do
	    res+=`java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.$lang $i`
	    res+="    "
	done;
	echo $res
    done;
done;
