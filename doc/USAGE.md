# Usage
Example queries are built for collections.

## Clojure code
You can use YZ from Clojure something like this:

```clojure
(ns some.ns
  (:require (ru.petrsu.nest.yz [core :as yzc] [yz-factory :as yzf])))

(def s-em 
  "Defines collection element manager with list some of strings."
  (yzf/c-em ["first" "second" ""] [String]))

(defn f
  "Returns list of non-empty strings."
  []
  (:rows (yzc/pquery "string#(empty=false)" s-em)))
```


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

## Java code

```java
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
```


## HelloWorld tutotial

* Create new project (lein is used):
	<pre><code>
	vdim@laptop:~/$ lein new yztest

	Created new project in: /home/vdim/yztest
	vdim@laptop:~/$ cd yztest/
	vdim@laptop:~/yztest$ ls
	project.clj  README  src  test
	vdim@laptop:~/yztest$
	</code></pre>

* Add yz to dependencies (file project.clj):

```clojure
(defproject yztest "1.0.0-SNAPSHOT"
    :description "Test application."
    :dependencies [[org.clojure/clojure "1.3.0"]
                   [ru.petrsu.nest/yz "0.0.1-alpha11"]])
```
* Download dependencies:
	<pre><code>
	vdim@laptop:~/yztest$ lein deps

	Some long output...

	vdim@laptop:~/yztest$ ls lib/
	algo.monads-0.1.0.jar  clojure-1.3.0.jar  hibernate-jpa-2.0-api-1.0.1.Final.jar  tools.macro-0.1.0.jar  yz-0.0.1-alpha11.jar
	vdim@laptop:~/yztest$
	</code></pre>

* Check whether YZ works:

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

