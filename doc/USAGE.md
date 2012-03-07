# Usage
Examples are built for queries for collection.

## Clojure code
You can use YZ from Clojure something like this:

	(ns some.ns
	  (:require (ru.petrsu.nest.yz [core :as yzc] [yz-factory :as yzf])))

	(def s-em 
	    "Defines collection element manager with some
	    list of strings."
	    (yzf/c-em ["first" "second" ""] [String]))

	(defn f
	    "Returns list of non-empty strings."
	    []
	    (:rows (yzc/pquery "string#(empty=false)" s-em)))


## Using from REPL of the YZ test project

	vdim@laptop:~/yz$ cd test
	vdim@laptop:~/yz/test$ lein repl
	user=> (in-ns 'ru.petrsu.nest.yz.init)
	#<Namespace ru.petrsu.nest.yz.init>
	ru.petrsu.nest.yz.init=>
	ru.petrsu.nest.yz.init=> (def s-em (c-em ["first" "second" ""] [String]))
	#'ru.petrsu.nest.yz.init/s-em
	ru.petrsu.nest.yz.init=>
	ru.petrsu.nest.yz.init=> (pquery "string" s-em)
	#ru.petrsu.nest.yz.core.Result{:result [["first" [] "second" [] "" []]], :error nil, :columns ("String"), :rows (["first"] ["second"] [""])}
	ru.petrsu.nest.yz.init=>
	ru.petrsu.nest.yz.init=> (pquery "string#(empty=false)" s-em)
	#ru.petrsu.nest.yz.core.Result{:result [["first" [] "second" []]], :error nil, :columns ("String"), :rows (["first"] ["second"])}
	ru.petrsu.nest.yz.init=>
	ru.petrsu.nest.yz.init=> (pquery "@(count `string')" s-em)
	#ru.petrsu.nest.yz.core.Result{:result [[3 []]], :error nil, :columns ("Long"), :rows ([3])}
	ru.petrsu.nest.yz.init=>


## Java code

	package yzusage;

	import ru.petrsu.nest.yz.QueryYZ;
	import ru.petrsu.nest.yz.YZFactory;
	
	public class YZTest {
	    public static void main(String args[]) {
		// Define our collection.
		List<String> l = new ArrayList<String>();
		l.add("first");
		l.add("second");
		l.add("");

		// Define list with classes.
		List<Class> cls = new ArrayList<Class>();
		cls.add(String.class);

		QueryYZ yz = YZFactory.createCollectionQueryYZ(l, cls);
		System.err.println("Amount non-empty strings: " + yz.getResultList("string#(empty=false)").size());
	    }
	}


## HelloWorld tutotial

1. Create new project (lein is used):
	<pre><code>
	vdim@laptop:~/$ lein new yztest

	Created new project in: /home/vdim/yztest
	vdim@laptop:~/$ cd yztest/
	vdim@laptop:~/yztest$ ls
	project.clj  README  src  test
	vdim@laptop:~/yztest$
	</code></pre>

2. Add yz to dependencies (file project.clj):

	<pre><code>
	(defproject yztest "1.0.0-SNAPSHOT"
	    :description "Test application."
  	    :dependencies [[org.clojure/clojure "1.3.0"]
                	   [ru.petrsu.nest/yz "0.0.1-alpha11"]])

3. Download dependencies:
	<pre><code>
	vdim@laptop:~/yztest$ lein deps

	Some long output...

	vdim@laptop:~/yztest$ ls lib/
	algo.monads-0.1.0.jar  clojure-1.3.0.jar  hibernate-jpa-2.0-api-1.0.1.Final.jar  tools.macro-0.1.0.jar  yz-0.0.1-alpha11.jar
	vdim@laptop:~/yztest$
	</code></pre>

4. Check whether YZ works:

	<pre><code>
	vdim@laptop:~/yztest$ lein repl
	REPL started; server listening on localhost:49333.
	user=> (use 'ru.petrsu.nest.yz.core)
	nil
	user=> (use 'ru.petrsu.nest.yz.yz-factory)
	nil
	user=> (def s-em (c-em ["first" "second" ""] [String]))
	#'user/s-em
	user=> (pquery "string" s-em)
	#ru.petrsu.nest.yz.core.Result{:result [["first" [] "second" [] "" []]], :error nil, :columns ("String"), :rows (["first"] ["second"] [""])}
	user=>
	</code></pre>

