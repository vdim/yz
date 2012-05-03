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
heap_mem=`java -cp $CP clojure.main -e "$code"`

echo "Default value = " $heap_mem;

# Defines memory's value which will be added/subtracted to/from current value of heap memory. 
diff_mem=$heap_mem

while true; do
    # DON'T REARRANGE NEXT TWO LINES (because of the command test uses the $? variable).
    diff_mem=`bc <<< "$diff_mem / 2"`
    ./bench_indq.sh -j "-Xmx$heap_mem" -l yz -t hdd -d lsm -q 0 -n "1000" >& /dev/null
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

echo "Final value = " $last_success_mem;

