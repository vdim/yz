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

# Classpath
CP=`lein classpath`

# Default heap memory
code="(.. java.lang.management.ManagementFactory getMemoryMXBean getHeapMemoryUsage getMax)"
default_heap_mem=`java -cp $CP clojure.main -e "$code"`

echo "Default value = " $default_heap_mem;

# Default sets of parameters of query
langs="yz hql"
dbs="h2 lsm"
n_dbs="1000 10000"

for lang in $langs; do # cycle by languages
for q in `seq 0 6`; do # cycle by queries
for db in $dbs; do # cycle by databases
for n in $n_dbs; do # cycle by count elements in database
    
    heap_mem=$default_heap_mem
    # Defines memory's value which will be added/subtracted to/from current value of heap memory. 
    diff_mem=$heap_mem
    
    echo "settings = " $lang $db $q $n 

    while true; do
	# DON'T REARRANGE NEXT TWO LINES (because of the command test uses the $? variable).
	diff_mem=`bc <<< "$diff_mem / 2"`
	./bench_indq.sh -j "-Xmx$heap_mem" -l $lang -t hdd -d $db -q $q -n $n >& /dev/null
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

    echo $lang $db $q $n $last_success_mem >> mem_test.txt
done # cycle by count elements in database
done # cycle by databases
done # cycle by queries
done # cycle by languages

