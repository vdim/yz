#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University. 
#
# This file is part of YZ.
#
# Benchmarks memory usage.
#


# Defines default presion of the benchmark (in bytes).
PRECISION=131072 # 128 KByte

# Initial value of the default heap memory.
# It is defined as 0 before handling options because of
# in case user specifies help option, user will not wait
# executing the "lein classpath" commant.
default_heap_mem=0

# Default sets of parameters of query
langs="yz hql"
db_types="ram hdd"
dbs="h2"
n_dbs="1000 5000 10000 15000"
qs=`seq 0 6`

# Output file
output="a.out"

# Help string
usage="Usage: $0 [OPTION...]
Benchmark amount of memory which is usage during for queries.

Options:
    -l, --langs <\"l1 ...\">	  list of languages. \"hql yz\" by default.
    -t, --dbtypes <\"t1 ...\">    list of types of database. \"ram hdd\" by default.
    -d, --databases <\"d1 ...\">  list of databases (h2, derby, hsqldb, lsm). h2 by default.
    -q, --query-nums <\"n1 ...\"> list of numbers of query.  \"0 1 2 3 4 5 6\" by default.
    -n, --elems-database <\"el1 ...\"> list with amount elements into databases.
    -m, --default-heap-mem <num>  default value of the size of heap memory in bytes.
    -p, --precision <num>	  defines value of precision in bytes (128KByte by default).
    -o, --output		  name of an output file (a.out by default).
    -h, --help		          display this help message and exit."

# Handling options.
while true; do
    case "$1" in
        -l|--langs) langs="$2"; shift 2 ;;
        -t|--dbtypes) db_types="$2"; shift 2 ;;
        -d|--databases) dbs=$2; shift 2;;
        -q|--query-nums) qs=$2; shift 2;;
        -h|--help) echo "$usage"; exit 0 ;; 
	-n|--elems-database) n_dbs=$2; shift 2;;
	-m|--default-heap-mem) default_heap_mem=$2; shift 2;;
	-p|--precision) PRECISION=$2; shift 2;;
	-o|--output) output=$2; shift 2;;
        -*) echo "unknown option $1" >&2 ; exit 1 ;;
	*) break ;;
    esac
done

# Initial value of last success memory. If value is 0 then success is not reached.
last_success_mem=0

# Define default value of the size of heap memory.
if test $default_heap_mem -eq 0; then
    # Classpath
    CP=`lein classpath`

    # Default heap memory is known due to the MemoryMXBean instance 
    # which is created during running clojure code.
    code="(.. java.lang.management.ManagementFactory getMemoryMXBean getHeapMemoryUsage getMax)"
    default_heap_mem=`java -cp $CP clojure.main -e "$code"`
fi

echo "Initial settings = " $langs $db_types $dbs $qs $n_dbs; 
echo "Default value = " $default_heap_mem;
    
for lang in $langs; do # cycle by languages
for q in $qs; do # cycle by queries
for db in $dbs; do # cycle by databases
for n in $n_dbs; do # cycle by count elements in database
for db_type in $db_types; do # cycle by type of database
    
    heap_mem=$default_heap_mem
    # Defines memory's value which will be added/subtracted to/from current value of heap memory. 
    diff_mem=$heap_mem
    
    while true; do
	# DON'T REARRANGE NEXT TWO LINES (because of the command test uses the $? variable).
	diff_mem=`bc <<< "$diff_mem / 2"`
	./bench_indq.sh -j "-Xmx$heap_mem" -l $lang -t $db_type -d $db -q $q -n $n >& /dev/null

	if test $? -ne 0; then
	    heap_mem=`bc <<< "$heap_mem + $diff_mem"`
	else
	    last_success_mem=$heap_mem
	    heap_mem=`bc <<< "$heap_mem - $diff_mem"`
	fi;
    
	# If difference is less or equal than PRECISION, then searching is finished.
	if test $diff_mem -le $PRECISION; then
	    break;
	fi;
    done

    echo $lang $db $q $n $last_success_mem >> $output
done # cycle by type of database
done # cycle by count elements in database
done # cycle by databases
done # cycle by queries
done # cycle by languages
