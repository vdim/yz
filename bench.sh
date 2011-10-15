#!/bin/bash

CP=`lein classpath`
LANGS="yz hql"
N=5

for i in `seq 0 $N`; do 
    res=""
    for lang in $LANGS; do
	res+=`java -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.$lang $i`
	res+="    "
    done;
    echo $res
done;
