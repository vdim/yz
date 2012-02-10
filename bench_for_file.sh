#!/bin/bash
#
# Copyright (C) 2011-2012 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Runs benchmark for specified set of amount database elements.
#

n_bd="1000 5000 10000 15000 20000 50000 100000"

## Java options.
JAVA_OPTS=""

## Classpath
CP=`lein classpath`

## Files with results of benchmarks
FILE_NEW="etc/yz-bench-new.txt"
FILE_LIST="etc/yz-bench-list.txt"
FILE_HQL="etc/hql-bench-list.txt"
FILE_JPA="etc/jpa-bench-list.txt"
FILE_OTH=""
FILE=$FILE_JPA

## MOM
MOM_JPA="nest_jpa.mom"
MOM_MEM="nest.mom"
MOM=$MOM_JPA

## Modificators: hql, jpa, jpa-list, list, lsm, lsm-list

for n in $n_bd; do
	if test $n -gt 20000; then
	    JAVA_OPTS="-Xss256M -Xmx2G"
	fi;
        java $JAVA_OPTS -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bench-norepl $MOM $n 50 $FILE "jpa-list"
done;

