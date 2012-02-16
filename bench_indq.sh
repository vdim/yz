#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University.
#
# Benchmarks individual-queries lists with queries from 
# ru.petrsu.nest.yz.benchmark.hql and ru.petrsu.nest.yz.benchmark.yz namespaces.
#


## Classpath
CP=`lein classpath`


## Derby settings.
url="jdbc:derby:db-NUM;create=true"
dialect="org.hibernate.dialect.DerbyDialect"
driver="org.apache.derby.jdbc.EmbeddedDriver"
derby="$url $dialect $driver"

## H2 settings.
url_h2="jdbc:h2:db-h2-NUM/db"
url_h2_mem="jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE;create=true"
dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"
h2="$url_h2_mem $dialect_h2 $driver_h2"


## HSQLDB settings.
url_hsqldb="jdbc:hsqldb:db-hsqldb-NUM/db"
dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
driver_hsqldb="org.hsqldb.jdbcDriver"
hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"

## Current database settings.
db=$h2

n_bd="1000 5000 10000 15000 20000 50000 100000"

clj_file="test/ru/petrsu/nest/yz/benchmark/benchmark.clj"
clj_func="ru.petrsu.nest.yz.benchmark.benchmark/bench-ind-query"

for n in $n_bd; do
    java -cp $CP clojure.main -i $clj_file -e "($clj_func -1 \"mem\" \"$db\" \"h2\" $n)" 
done;

