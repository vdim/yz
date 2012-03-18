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

# Default measurement
measurement="time"

# Default idle counts of calling query
idle_count=0

# Default bb library usage.
library_bb=false

# Help string
usage="Usage: $0 [OPTION...]
Benchmark individual queries.

Options:
    -l, --lang <lang>	    language of benchmark (yz or hql). hql by default.
    -t, --db_type <type>    type of database (ram or hdd). ram by default.
    -d, --database <database> database (h2, derby, hsqldb, lsm). h2 by default.
    -q, --query-num <num>   number of query (use -1 for benchmarking all queries) 
			    from vector. -1 by default.
    -n, --elems-database <\"el1 ...\"> list with amount elements into databases
    -c, --count <num>	    count of execution. 1 by default.
    -p, --prefix <prefix>   prefix for files for result of benchmark. Empty by default.
    -b, --label <label>	    addition label for chart's legend.
    -j, --java-options <\"options\"> define java options.
    -m, --measurement <measure> type of measurement (time, thread-time-cpu, thread-time-user, memory).
			    time by default.
    -i, --idle <num>	    idle count of calling query. 0 by defaul.
    -B, --library-bb	    define whether bb library must be used for benchmark. false by default.
    -h, --help		    display this help message and exit."

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
	-m|--measurement) measurement="$2"; shift 2;;
	-i|--idle) idle_count="$2"; shift 2;;
	-B|--library-bb) library_bb="true"; shift 1;;
        -*) echo "unknown option $1" >&2 ; exit 1 ;;
	*) break ;;
    esac
done

# Variable 
url="${database}_${db_type}"

# Classpath
CP=`lein classpath`

for i in `seq $c`; do
    echo $i
    for n in $n_db; do

	# Derby url.
	derby_hdd="jdbc:derby:db-$n"
	derby_ram="jdbc:derby:memory:db;create=true";
	derby="${!url} $dialect_derby $driver_derby"

	# H2 url.
	h2_hdd="jdbc:h2:db-h2-$n/db"
	h2_ram="jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;MVCC=TRUE;create=true"
	h2="${!url} $dialect_h2 $driver_h2"
	
	# HSQLDB url.
	hsqldb_hdd="jdbc:hsqldb:db-hsqldb-$n/db"
	hsqldb_ram="jdbc:hsqldb:mem:db;create=true"
	hsqldb="${!url} $dialect_hsqldb $driver_hsqldb"
	
	# LocalSonManager url.
	lsm=$database":data-$n"

	# Define current connection string.
	conns=${!database}
	
	params="\"$lang\" $q_num \"$db_type\" \"$conns\" \"$lang-$db_type-$database$label\" \
	       $n \"$prefix\" \"$measurement\" $idle_count $library_bb"

	# Run bench-ind-query function from ru.petrsu.nest.yz.benchmark.benchmark namespace. 
	# For more details see doc string for the clojure.main/main function.
	# For more details about parameters of the bench-ind-query function see doc string for
	# ru.petrsu.nest.yz.benchmark.benchmark/bench-ind-query.
	java $JAVA_OPTIONS -cp $CP clojure.main -i $clj_file -e "($clj_func $params)"
    done;
done;
