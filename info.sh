#!/bin/bash
#
# Copyright (C) 2011 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Script for getting information about machine (needed for benchmarking).
#

LASTCOMMIT=`git log -1|grep commit|cut -f 2 -d' '`
MEMTOTAL=`cat /proc/meminfo|grep MemTotal|cut -f 2 -d ':'`
INFO=`uname -nsm`

# Counts cpus and outputs number of cpus and its model.
MODEL_NAME=`cat /proc/cpuinfo|grep "model name"|cut -f 2 -d ':'|uniq -c`

echo $LASTCOMMIT $MODEL_NAME $CACHE_SIZE $MEMTOTAL $INFO

