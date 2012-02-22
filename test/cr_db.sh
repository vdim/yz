#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Generates databases.
#

# Set with amount elements of database.
n_db="1000 5000 10000 15000 20000 50000 100000"

clj_file="src/ru/petrsu/nest/yz/benchmark/benchmark.clj"
clj_func="ru.petrsu.nest.yz.benchmark.benchmark/generate-bd"

# Derby settings.
dialect_derby="org.hibernate.dialect.DerbyDialect"
driver_derby="org.apache.derby.jdbc.EmbeddedDriver"

# H2 settings.
dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"

# HSQLDB settings.
dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
driver_hsqldb="org.hsqldb.jdbcDriver"

# Define default database.
database="h2"

# Help string
usage="Usage: $0 [OPTION...]
Generate database of specified type for specified amount elements.

Options:
    -d, --database <database>   database (h2, derby, hsqldb, lsm). h2 by default.
    -n, --elems-database <\"el1 el2 ...\"> list with amount elements into databases
				(all by default).
    -h, --help			display this help message and exit"

# Handling options.
while true; do
    case "$1" in
        -d|--database) database=$2; shift 2;;
        -h|--help) echo "$usage"; exit 0 ;; 
	-n|--elems-database) n_db=$2; shift 2;;
        -*) echo "unknown option $1" >&2 ; exit 1 ;;
	*) break ;;
    esac
done


# Classpath
CP=`lein classpath`

for n in $n_db; do
	echo $n

	# Derby url.
	url_derby="jdbc:derby:db-$n;create=true"
	derby="$url_derby $dialect_derby $driver_derby"

	# H2 url.
	url_h2="jdbc:h2:db-h2-$n/db"
	h2="$url_h2 $dialect_h2 $driver_h2"

	# HSQLDB url.
	url_hsqldb="jdbc:hsqldb:file:db-hsqldb-$n/db;shutdown=true;hsqldb.write_delay=false"
	hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"

	# LocalSonManager url.
	url_lsm="data-$n"
	lsm="$url_lsm"

	# Define current connection string.
	url=$h2
	case "$database" in
	    "h2") url=$h2 ;;
	    "derby") url=$derby ;;
	    "hsqldb") url=$hsqldb ;;
	    "lsm") url=$lsm ;;
	esac

	# Run generate-bd function from ru.petrsu.nest.yz.benchmark.benchmark namespace. 
	# For more details see doc string for the clojure.main/main function.
	java -cp $CP clojure.main -i $clj_file -e "($clj_func $n \"$url\")" 
done;

