## Supported backends
At this moment implementation of YZ for java supports the following backends:

* [Collections] (http://docs.oracle.com/javase/tutorial/collections/).
* [JPA] (http://jcp.org/aboutJava/communityprocess/final/jsr317/index.html).

## High-level architecture
High-level architecture of the YZ implementation for Java is represented on figure below:
<img src="https://github.com/vdim/yz/raw/master/doc/hl-arch.png" alt="High-level architecture"/>

Notes:

* Query is parsed by parser 
([sourse] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/parsing.clj)) 
which is based on [fnparse] ( https://github.com/joshua-choi/fnparse) 
library which is based on parser combinators which is based on [monads] (http://intensivesystems.net/tutorials/monads_101.html).

* Parsing of query is based on Map of Object Model (MOM). See section MOM in this document below for more detail. 

* Parser produces some inner data structure and passes it to query evaluator 
([sourse] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/core.clj)).

* Query evaluator uses some implementation of the ElementManager for accessing to data. 
At this moment there is the following ElementManagers:
    * JPA ElementManager which uses the JPA API 2.0 for accessing to data. We test it by Hibernate.
    * Collection ElementManager which allows querying to usual Java collections.
    * Store ElementManager - inner storage of the Nest project.

In case you want to add your data storage you must implement ElementManager interface. See
definition of it [here] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/core.clj) 
for more details.

* Query evaluator returns map where 
    * :result - hierarchical structure of result query.
    * :error - if error is occured then value of this keyword contains string representation of the error. If not then value is nil.
    * :columns - list with default names of columns.
    * :rows - representation of a result query as set of rows (tuples).

## MOM
MOM may contain the following information:
* classes of your model
* paths between classes
* brief names of classes
* default properties
* comparators (it may be useful in case you can not change your model, but you want to sort your selections and 
this classes are not implement [Comparable] (http://docs.oracle.com/javase/6/docs/api/java/lang/Comparable.html) 
interface).

You can see example of MOM for the SON model [here] (https://github.com/vdim/yz/blob/master/test/test-resource/nest.mom).

### Creating MOM
In order to create MOM automatically (at least skeleton) you can use the 
[gen-mom] (https://github.com/vdim/yz/blob/master/gen_mom.sh) script.

See

    ./gen_mom.sh -h

for more details.


