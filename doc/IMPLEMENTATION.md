This document contains information about implementation of the YZ in [Clojure] (http://clojure.org/).

* <a href="#hl_arch">High-level architecture</a>
* <a href="#mom">MOM</a>
* <a href="#details">Some another technical details</a>
* <a href="#backends">Supported backends</a>

<a name="hl_arch"></a>
## High-level architecture
High-level architecture of the YZ implementation for Clojure/Java is represented on figure below:
<img src="https://github.com/vdim/yz/raw/master/doc/hl-arch.png" 
alt="High-level architecture" title="High-level architecture"/>

Notes:

* Query is parsed by parser 
([sourse] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/parsing.clj)) 
which is based on [fnparse] ( https://github.com/joshua-choi/fnparse) 
library which is based on [parser combinators] (http://en.wikipedia.org/wiki/Parser_combinator)
which is based on [monads] (http://www.intensivesystems.net/tutorials/monads_101.html).

* Parsing of query is based on Map of Object Model (MOM). See section MOM in this document below for more details.

* Parser produces some inner data structure and passes it to query evaluator 
([sourse] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/core.clj)).

* Query evaluator uses some implementation of the ElementManager for accessing to data. 

* The pquery function from [core.clj](https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/core.clj) returns map where 
    * :result - hierarchical structure of result query.
    * :error - if error is occured then value of this keyword contains string representation of the error. If not then value is nil.
    * :thrwable - if error is occured then value of this keyword is Throwable object.
    * :columns - list with default names of columns.
    * :rows - representation of a result query as set of rows (tuples).


<a name="mom"></a>
## MOM
MOM may contain the following information:

* classes of model;
* paths between classes;
* brief names of classes;
* information about default properties;
* comparators (it may be useful in case you can not change your model, but you want to sort your selection and 
this class does not implement [Comparable] (http://docs.oracle.com/javase/6/docs/api/java/lang/Comparable.html) 
interface). Note that comparator from MOM has more high priority than implementation of the Comparable interface.
* namespaces for functions.

In point of view of the Clojure data structure MOM is map where keys are classes of 
model and values are maps with information. You can see example of MOM
[here] (https://github.com/vdim/yz/blob/master/test/test-resource/nest.mom).

### Creating MOM
In order to create MOM automatically (at least skeleton) you can use the 
[gen-mom] (https://github.com/vdim/yz/blob/master/gen_mom.sh) script.

See

    ./gen_mom.sh -h

for more details.

At this moment you should correct given map manually. There is 
[project] (https://github.com/vdim/yz/blob/master/mom-editor) which goal is
creating editor for MOM, but now it is useless.


<a name="details"></a>
## Some another technical details

* In case there are several paths between two classes then first shortest is used. 
This question requires deep researching.
* Sorting requires that your objects (or property) must implement
[Comparable] (http://docs.oracle.com/javase/6/docs/api/java/lang/Comparable.html) interface. 
* Comparison for "where" clause works correctly for objects which implement
[Comparable] (http://docs.oracle.com/javase/6/docs/api/java/lang/Comparable.html) interface. 
You can define comparator in the MOM (see above section) as well.
* For implementation "removing duplicates" feature 
[distinct] (http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/distinct) function of Clojure is used.
* Operator == is implemented due to 
[identical?] (http://clojure.github.com/clojure/clojure.core-api.html#clojure.core/identical?) function of Clojure.


<a name="backends"></a>
## Supported backends
At this moment implementation of the YZ supports the following backends:

* [Collections] (http://docs.oracle.com/javase/tutorial/collections/).
* [JPA] (http://jcp.org/aboutJava/communityprocess/final/jsr317/index.html).

So there are the following ElementManagers respectively:

* Collection ElementManager which allows querying to usual Java collections.
* JPA ElementManager which uses the JPA API 2.0 for accessing to data. We test it by Hibernate.

In case you want to add your data storage you must implement ElementManager interface. See
definition of it [here] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/core.clj) 
for more details.
