#!/bin/bash
#
# Copyright (C) 2011-2012 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Runs benchmark for specified set of amount database elements.
# Query or name of list with queries are specified into file.
#
# 20.02.2012 08:34 @DEPRECATED! Use bench_indq.sh for benchmarking.
#

# Amount elements into DB by default.
n_db="1000 5000 10000 15000 20000 50000 100000"

# Java options.
JAVA_OPTS=""

# File with result of benchmark.
file="test.txt"

# MOM
MOM_JPA="nest_jpa.mom"
MOM_MEM="nest.mom"
MOM=$MOM_MEM

# Language by default
lang="hql"

# Type of benchmark by default.
b_type="list"

# Help string
usage="Usage: $0 [OPTION...]
Benchmark queries (or list with queries) which are specified into file.

Options:
    -f, --file <file>	    file with queries (or name of list with queries) and for	
			    for result of benchmark. test.txt by default.
    -l, --lang <lang>	    language of benchmark (yz or hql). hql by default.
    -b, --btype <type of benchmark> defines whether file contains individual queries
			    (ind) or names of list with queries (list). list by default
    -t, --dbtype <type>    type of database (mem or hdd). mem by default.
    -d, --database <database> database (h2, derby, hsqldb, lsm). h2 by default.
    -n, --elems-database    list with amount elements into databases
    -h, --help		    display this help message and exit"

# Handling options.
while true; do
    case "$1" in
        -f|--file) file="$2"; shift 2 ;;
        -l|--lang) lang="$2"; shift 2 ;;
        -t|--dbtype) db_type="$2"; shift 2 ;;
        -b|--btype) b_type=$2; shift 2;;
        -h|--help) echo "$usage"; exit 0 ;; 
	-n|--elems-database) n_db=$2; shift 2;;
        -*) echo "unknown option $1" >&2 ; exit 1 ;;
	*) break ;;
    esac
done
## Classpath
CP=`lein classpath`

clj_file="ru.petrsu.nest.yz.benchmark.bench-norepl"

## Modificator of database: jpa, lsm (get database from "data-NUM" directory), 
## lsm-gen (use in case you want to generate database),  mem.
## Modificator of function: list, ind
## Modificator of language: hql, yz.
## 
## Note: lsm and jpa database compatible only with yz language.

for n in $n_db; do
    echo $n
    java $JAVA_OPTS -cp $CP clojure.main --main $clj_file $MOM $n 1 $file "lsm" $b_type $lang
done;

