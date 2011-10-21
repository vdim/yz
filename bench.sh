#!/bin/bash
#
# Copyright (C) 2011 Vyacheslav Dimitrov, Petrozavodsk State University
#
# Script for benchmarking and creating report.
#

## Prints help.
show_help ()
{
	echo "Usage: ."
	echo "	-r datereport1[,datereport2]...[,datereportN] Create report."
	echo "		    If dateport greather than 1 then compare report is created.";
	echo "	-n No bench."
	echo "	-s If this option supplied then bench use one database (not for each count elements.)."
	echo "	-g Generates database."
}


NO_BENCH="0";
REPORT=""

## Defines whether generating database.
GENERATE_DB="0"

## For each count of elements separate database.
SUFFIX="1"

## Parse options.
while getopts r:nhgs OPT
do
    case ${OPT} in
    r)    REPORT="${OPTARG}";
	  echo "Not supported yet.";
	  exit 0;;
    n)    NO_BENCH="1";;
    g)    GENERATE_DB="1";;
    s)    SUFFIX="0";;
    h)    show_help;
          exit 0;;
    \?)   echo "Unknown or uncomplete command line option.";
          echo "Use -h to get help.";
          exit 2;;
    esac
done


## Classpath
CP=`lein classpath`

## List of languages or technologies for quering.
LANGS="yz hql"

## Number of queries.
N=13

## Name of the persistence unit.
name="bench"

## List with number of elements for DB.
counts="100 1000 10000 25000 50000 75000 100000 250000 300000 400000 500000"

## Java options (Needed for big databases (since 250000 elements)).
JAVA_OPTS="-Xss128M -Xmx1G"


## Function which create report.
do_report() {
    echo "Do report..."

    logdir=$1	
    cd $logdir
    ## Create report directory.
    repdir="report/figures"
    mkdir -p $repdir || echo "Could not create directory $repdir" exit 1;
	
    ## Create graphics.
    for i in `seq 0 $N`; do 
	../../doplot.sh $i
    done;

    cd report
    cp ../../../etc/report/document.tex ./

    pdflatex document.tex > /dev/null 2>&1

    cd ../../../
}


if test "$#" -gt 1; then
    if [ $1 = "--nobench" ]; then
	do_report "logs/$2"
	exit 0;
    fi;
fi;


echo "Execute queries..."

## Create logs directory if any.
logdir="logs"
if [ ! -d $logdir ]; then
    mkdir $logdir || echo "Could not create directory $logdir" exit 1;
fi;

## Create logs directory for current benchmark.
logdir+=\/`date +%Y%m%d%H%M%S`/
mkdir $logdir || echo "Could not create directory $logdir" exit 1;

for c in $counts; do 

    suf="1"
    if test "$SUFFIX" = "1"; then
	suf="_"$c;
    fi;

    ## Derby settings.
    url="jdbc:derby:db"$suf";create=true"
    dialect="org.hibernate.dialect.DerbyDialect"
    driver="org.apache.derby.jdbc.EmbeddedDriver"
    derby="$url $dialect $driver"

    ## H2 settings.
    url_h2="jdbc:h2:db"$suf"/db1;create=true"
    dialect_h2="org.hibernate.dialect.H2Dialect"
    driver_h2="org.h2.Driver"
    h2="$url_h2 $dialect_h2 $driver_h2"


    ## HSQLDB settings.
    url_hsqldb="jdbc:hsqldb:db"$suf"/db1;create=true"
    dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
    driver_hsqldb="org.hsqldb.jdbcDriver"
    hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"

    ## Current settings.
    db=$h2
 
    echo $db   
    # Generate the DB with specified amount of elements.
    if test "$GENERATE_DB" = "1"; then
	java $JAVA_OPTS -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bd-utils $c $name $db
    fi;

    # Execute query.
    if test "$NO_BENCH" = "0"; then
	for i in `seq 0 $N`; do 
	    res="$c "
	    for lang in $LANGS; do
		res+="`java $JAVA_OPTS -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.benchmark $i $name $db $lang` "
	    done;
	    echo $res >> $logdir/$i.txt;
	done;
    fi;
done;


## Do report if any.
if test "$#" -gt 0; then
    if [ $1 = "--doreport" ]; then
	do_report $logdir
    fi;
fi;
