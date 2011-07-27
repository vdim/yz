#!/bin/bash

#
# Modified version from DaCoPAn CVS.
# Author unknown.
#
# Alexander Kolosov, 2009-10-26: 
#     recoding the bbl-file compiled by bibtex command from koi8-r to utf-8
#

docname=$1
prev_toc_md5sum="X"
if [ -e $docname.toc ]; then
	toc_md5sum=`md5sum $docname.toc`
fi
maxruns=10
while [ "z$prev_toc_md5sum" != "z$toc_md5sum" -a $maxruns -gt 0 ]; do
	pdflatex -interaction nonstopmode $docname.tex
	if [ "$?" != 0 ]; then
		exit 1
	fi
	prev_toc_md5sum=$toc_md5sum
	toc_md5sum=`md5sum $docname.toc`
	if fgrep 'Citation' $docname.log ; then
		bibtex $docname
		recode koi8-r..utf-8 $docname.bbl
		prev_toc_md5sum="X" 
	fi
	let maxruns--
done
if [ $maxruns -eq 0 ]; then
	echo "==="
	echo "Maximum number of runs exceeded, probably stuck in infinite loop"
	exit 1
fi
while fgrep -q 'Rerun to get' $docname.log ; do
	pdflatex -interaction nonstopmode $docname.tex
	if [ "$?" != 0 ]; then
		exit 1
	fi
done
