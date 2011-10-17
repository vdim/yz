#!/bin/bash

## Classpath
CP=`lein classpath`

## List of languages or technologies for quering.
LANGS="yz hql"

## Number of queries.
N=9

## Name of the persistence unit.
name="bench"

## Derby settings.
url="jdbc:derby:db1;create=true"
dialect="org.hibernate.dialect.DerbyDialect"
driver="org.apache.derby.jdbc.EmbeddedDriver"
derby="$url $dialect $driver"

## H2 settings.
url_h2="jdbc:h2:db1/db1;create=true"
dialect_h2="org.hibernate.dialect.H2Dialect"
driver_h2="org.h2.Driver"
h2="$url_h2 $dialect_h2 $driver_h2"


## HSQLDB settings.
url_hsqldb="jdbc:hsqldb:db1/db1;create=true"
dialect_hsqldb="org.hibernate.dialect.HSQLDialect"
driver_hsqldb="org.hsqldb.jdbcDriver"
hsqldb="$url_hsqldb $dialect_hsqldb $driver_hsqldb"

## Current settings.
db=$h2

## List with number of elements for DB.
counts="100 1000 10000 25000 50000 75000 100000 250000 500000 750000 1000000"

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

    # Generate the DB with specified amount of elements.
    java $JAVA_OPTS -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.bd-utils $c $name $db

    # Execute query.
    for i in `seq 0 $N`; do 
	res="$c "
	for lang in $LANGS; do
	    res+="`java $JAVA_OPTS -cp $CP clojure.main --main ru.petrsu.nest.yz.benchmark.benchmark $i $name $db $lang` "
	done;
	echo $res >> $logdir/$i.txt;
    done;
done;


## Do report if any.
if test "$#" -gt 0; then
    if [ $1 = "--doreport" ]; then
	do_report $logdir
    fi;
fi;
