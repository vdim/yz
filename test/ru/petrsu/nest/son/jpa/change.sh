#!/bin/bash

bcp="_bcp"

for i in `ls *.java`; do
    #echo $i$bcp
    sed 's/^package ru.petrsu.nest.son;$/package ru.petrsu.nest.son.jpa;/' $i > $i$bcp
    cat $i$bcp > $i
    rm $i$bcp
done
