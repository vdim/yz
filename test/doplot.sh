#!/bin/bash

## Number of query.
num=$1

## File with data.
file="$num.txt"

code="set title 'Query #$num';
      set xlabel 'Num of elements.';
      set ylabel 'Time (ms)';
      set term png;
      set output 'report/figures/query$num.png';
      plot '$file' using 1:3 smooth unique t 'HQL' w lines, \
	   '$file' using 1:2 smooth unique t 'YZ' w linespoints"

gnuplot <<< $code
