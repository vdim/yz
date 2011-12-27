#!/bin/bash
#
# Copyright (C) 2011 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Runs benchmark for specified set of commits.
#

## Commits
commits="51a56985c73a6f5688ed491ad17a6964e3ea3d3c 
	51a56985c73a6f5688ed491ad17a6964e3ea3d3c"
#	f872faefdef67fdc2beca3e204e66f65e214f368 
#	2f8b91b67400892b82be1355e7f56e270874645a"

## Classpath
CP=`lein classpath`


for commit in $commits; do
	git checkout $commit
	lein clean && ant && lein compile && lein test
	java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bench-norepl nest.mom 1000 1 test.txt true
done;
