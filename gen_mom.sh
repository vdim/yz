#!/bin/bash
#
# Copyright (C) 2012 Vyacheslav Dimitrov, Petrozavodsk State University.
#
# Generates map of object model (MOM).
#



# Help string
usage="Usage: $0 [OPTION...]
Generates map of object model (MOM).

Options:
    -o, --output <file>	    A name of the output file. If -o is not supplied then a.mom is used.
    -i, --input <file>	    File with old MOM.
    -a, --append	    Define whether generated items will be append to old file 
			    (if -i is not supplied then -a is ignored). At that -o is ignored.
    -h, --help		    Display this help message and exit.

Source of classes (one must be defined): 
    -H, --hibernate-cfg <file>  List of classes will be get from xml file with hibernate configuration.
			    It will be useful in case this file contains mapping classes.
    -p, --persistense-unit <name> List of classes will be get from metamodel of ElementManagerFactory.
    -l, --list-classes <\"Class1 Class2 ...\"> Definition list of classes manually."

# Default output
output="a.mom"

# Old MOM
old_mom=""

# Source is empty.
src=""

# List of classes.
classes=""

# Append
append="false"

# Handling options.
while true; do
    case "$1" in
        -o|--output) output="$2"; shift 2 ;;
        -i|--input) old_mom="$2"; shift 2 ;;
        -a|--append) append="true"; shift 1 ;;
        -h|--help) echo "$usage"; exit 0 ;;
	-H|--hibernate-cfg) src=":hibernate-cfg"; classes="$2"; shift 2;;
	-p|--persistense-unit) src=":persistense"; classes="$2"; shift 2;;
	-l|--list-classes) src=":list-classes"; classes="$2"; shift 2;;
        -*) echo "unknown option $1" >&2 ; exit 1 ;;
	*) break ;;
    esac
done;

if [ -z $src ]; then
    echo "ERROR: source must be defined."
    echo -e "\n$usage"
    exit 1;
fi;

# Classpath
CP=`lein classpath`

# Clojure settings
clj_file="../src/ru/petrsu/nest/yz/hb_utils.clj"
clj_func="ru.petrsu.nest.yz.hb-utils/gen-mom*"
params="\"$output\" \"$old_mom\" $append $src \"$classes\""

# Run gen-mom* function from ru.petrsu.nest.yz.hb-utils namespace. 
# For more details about calling see doc string for the clojure.main/main function.
# For more details about parameters of the bench-ind-query function see doc string for
# ru.petrsu.nest.yz.hb-utils/gen-mom*.
java $JAVA_OPTIONS -cp $CP clojure.main -i $clj_file -e "($clj_func $params)"
