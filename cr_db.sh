#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Generates databases.
#

n_bd="1000 5000 10000 15000 20000"

## Classpath
CP=`lein classpath`

for n in $n_bd; do
	echo $n
        java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.benchmark $n
done;

