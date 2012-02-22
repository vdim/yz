#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University.
#
# Benchmarks individual-queries lists with queries from 
# ru.petrsu.nest.yz.benchmark.hql and ru.petrsu.nest.yz.benchmark.yz namespaces.
#


## Derby settings.
dialect_derby="org.hibernate.dialect.DerbyDialect"
driver_derby="org.apache.derby.jdbc.EmbeddedDriver"

## H2 settings.
dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"


## HSQLDB settings.
dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
driver_hsqldb="org.hsqldb.jdbcDriver"

# Set with amount elements into database.
n_db="1000 5000 10000 15000 20000 50000 100000"

clj_file="src/ru/petrsu/nest/yz/benchmark/benchmark.clj"
clj_func="ru.petrsu.nest.yz.benchmark.benchmark/bench-ind-query"

# Define default language
lang="hql"

# Define default type
db_type="ram"

# Define default database.
database="h2"

# Define default number of query.
# -1 is used for all queries.
q_num=-1

# Count of execution.
c=1

# Prefix is empty by default.
prefix=""

# Addition label for legend of chart is empty by default.
label=""

# Default java options.
JAVA_OPTIONS=""

# Help string
usage="Usage: $0 [OPTION...]
Benchmark individual queries.

Options:
    -l, --lang <lang>	    language of benchmark (yz or hql). hql by default.
    -t, --db_type <type>    type of database (ram or hdd). ram by default.
    -d, --database <database> database (h2, derby, hsqldb, lsm). h2 by default.
    -q, --query-num <num>   number of query (use -1 for benchmarking all queries) 
			    from vector. -1 by default.
    -n, --elems-database <\"el1 el2 ...\"> list with amount elements into databases
    -c, --count <num>	    count of execution. 1 by default.
    -p, --prefix <prefix>   prefix for files for result of benchmark. Empty by default.
    -b, --label <label>	    addition label for chart's legend.
    -j, --java-options <\"options\"> define java options.
    -h, --help		    display this help message and exit"

# Handling options.
while true; do
    case "$1" in
        -l|--lang) lang="$2"; shift 2 ;;
        -t|--dbtype) db_type="$2"; shift 2 ;;
        -d|--database) database=$2; shift 2;;
        -q|--query-num) q_num=$2; shift 2;;
        -h|--help) echo "$usage"; exit 0 ;; 
	-n|--elems-database) n_db=$2; shift 2;;
	-c|--count) c=$2; shift 2;;
	-p|--prefix) prefix=$2; shift 2;;
	-j|--java-options) JAVA_OPTIONS=$2; shift 2;;
	-b|--label) label="-$2"; shift 2;;
        -*) echo "unknown option $1" >&2 ; exit 1 ;;
	*) break ;;
    esac
done

# Classpath
CP=`lein classpath`

for i in `seq $c`; do
    echo $i
    for n in $n_db; do

	# Derby url.
	url_derby="jdbc:derby:db-$n"
	url_derby_ram="jdbc:derby:memory:db;create=true";
	if test "$db_type" = "ram"; then
	    derby="$url_derby_ram $dialect_derby $driver_derby"
	else
	    derby="$url_derby $dialect_derby $driver_derby"
	fi;

	# H2 url.
	url_h2="jdbc:h2:db-h2-$n/db"
	url_h2_ram="jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;MVCC=TRUE;create=true"
	if test "$db_type" = "ram"; then
	    h2="$url_h2_ram $dialect_h2 $driver_h2"
	else
	    h2="$url_h2 $dialect_h2 $driver_h2"
	fi;

	# HSQLDB url.
	url_hsqldb="jdbc:hsqldb:db-hsqldb-$n/db"
	url_hsqldb_ram="jdbc:hsqldb:mem:db;create=true"
	if test "$db_type" = "ram"; then
	    hsqldb="$url_hsqldb_ram $dialect_hsqldb $driver_hsqldb"
	else
	    hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"
	fi;

	# LocalSonManager url.
	url_lsm="data-$n"
	lsm="$url_lsm"

	# Define current connection string.
	url=$h2
	case $database in
	    "h2") url=$h2 ;;
	    "derby") url=$derby ;;
	    "hsqldb") url=$hsqldb ;;
	    "lsm") url=$lsm ;;
	esac

	params="\"$lang\" $q_num \"$db_type\" \"$url\" \"$lang-$db_type-$database$label\" $n \"$prefix\""
	
	# Run bench-ind-query function from ru.petrsu.nest.yz.benchmark.benchmark namespace. 
	# For more details see doc string for the clojure.main/main function.
	# For more details about parameters of the bench-ind-query function see doc string for
	# ru.petrsu.nest.yz.benchmark.benchmark/bench-ind-query.
	java $JAVA_OPTIONS -cp $CP clojure.main -i $clj_file -e "($clj_func $params)"
    done;
done;
