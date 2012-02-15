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

## File with result of benchmark.
FILE=""

## MOM
MOM_JPA="nest_jpa.mom"
MOM_MEM="nest.mom"
MOM=$MOM_MEM

## Modificator of database: jpa, lsm, mem.
## Modificator of function: list, ind
## Modificator of language: hql, yz.
## 
## Note: lsm and jpa database compatible only with yz language.

for n in $n_bd; do
	if test $n -gt 20000; then
	    JAVA_OPTS="-Xss256M -Xmx512M"
	fi;
        java $JAVA_OPTS -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bench-norepl $MOM $n 1 $FILE "mem" "list" "yz"
done;

