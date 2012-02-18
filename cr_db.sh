#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Generates databases.
# Usage: ./cr_db [db_name]
# db_hame may be h2, derby, hsqldb, lsm (lsm by default).
#

# Set with amount elements of database.
n_bd="1000 5000 10000 15000 20000 50000 100000"

## Classpath
CP=`lein classpath`

clj_file="test/ru/petrsu/nest/yz/benchmark/benchmark.clj"
clj_func="ru.petrsu.nest.yz.benchmark.benchmark/generate-bd"

## Derby settings.
dialect_derby="org.hibernate.dialect.DerbyDialect"
driver_derby="org.apache.derby.jdbc.EmbeddedDriver"

## H2 settings.
dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"

## HSQLDB settings.
dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
driver_hsqldb="org.hsqldb.jdbcDriver"

for n in $n_bd; do
	echo $n

	# Derby url.
	url_derby="jdbc:derby:db-$n;create=true"
	derby="$url_derby $dialect_derby $driver_derby"

	# H2 url.
	url_h2="jdbc:h2:db-h2-$n/db"
	h2="$url_h2 $dialect_h2 $driver_h2"

	# HSQLDB url.
	url_hsqldb="jdbc:hsqldb:db-hsqldb-$n/db"
	hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"

	# LocalSonManager url.
	url_lsm="data-$n"
	lsm="$url_lsm"

	# Define current connection string.
	url=$lsm
	if test "$#" -gt 0; then
	    case "$1" in
		"h2") url=$h2 ;;
		"derby") url=$derby ;;
		"hsqldb") url=$hsqldb ;;
		"lsm") url=$lsm ;;
	    esac
	fi;

	# Run generate-bd function from ru.petrsu.nest.yz.benchmark.benchmark namespace. 
	# For more details see doc string for the clojure.main/main function.
	java -cp $CP clojure.main -i $clj_file -e "($clj_func $n \"$url\")" 
done;

