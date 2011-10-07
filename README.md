Implementation of the YZ query language specification.

## Restriction
It is just prototype, not for production using.

## Installation

### Build from source code
1. Install [lein] (https://github.com/technomancy/leiningen) build tool.
2. Download source code from repository:
<pre>
<code>
$ git clone http://github.com/vdim/yz
$ cd yz
</code>
</pre>

3. Download dependencies:
<pre><code>$ lein deps</code></pre>

4. Compile code:
<pre><code>$ lein compile</code></pre>

5. Create jar file:
<pre><code>$ lein jar</code></pre>

### Install from repository
The YZ is distributed due to [clojars.org] (http://clojars.org), so
if you use some dependency manager ([ivy] (http://ant.apache.org/ivy/), 
[maven] (http://maven.apache.org/), [leiningen] (https://github.com/technomancy/leiningen) and so on) you
can use it for getting the YZ. For example, you can write for ivy 
something like this:
	<pre><code><dependency org="ru.petrsu.nest" name="yz" rev="0.0.1-alpha1"/><code></pre>

Direct link: [http://clojars.org/ru.petrsu.nest/yz] (http://clojars.org/ru.petrsu.nest/yz)

## Usage
You can use YZ from your clojure code something like this:

	(ns some.ns
	  (:import (javax.persistence Persistence))
	  (:require [ru.petrsu.nest.yz.core :as c] [ru.petrsu.nest.yz.hb-utils :as hu]))

	(def emf (Persistence/createEntityManagerFactory "test-model"))
	(def mom (hu/gen-mom-from-metamodel emf))
	(def em (.createEntityManager emf))

	(c/pquery "text-of-query" mom em)

## Notes about the YZ
The main goal of the YZ language is reducing the text of a query
in comparison with existing object query languages (e.g. OQL, HQL, JP-QL and so on).

For achieving this we offers following approaches:

1. Eliminates keywords (select, from, where and so on).
2. Eliminates indications of associations.
3. Using concise names of entities.
4. A default property.
5. Using the reduced notation for complex restrictions.

Let`s get an object model from the book ["Java persistence with Hibernate"] 
(http://www.amazon.com/Java-Persistence-Hibernate-Christian-Bauer/dp/1932394885). 
See source code in [http://downloads.jboss.org/hibernate/caveatemptor/] (http://downloads.jboss.org/hibernate/caveatemptor/).
(We use this model for the testing the YZ, so you can sources`s model into YZ`s repository.)

Caveatemptor`s model has the following structure: 

Let`s consider each point in more details.

### Eliminates keywords (select, from, where and so on).
Although keywords are made right english sentencies from queries it are verbose.

For example for getting all categories you should write HQL query like this
	select c from Category as c
or its reduced version:
	from Category.

For getting some properties of the Category you should write
	select c.version, c.name from Category as c
(Note: a reduced version of this query is not.)

So we offer to eliminate keywords. For example for getting all object of Category`s class you can just write
	category
Also you can define a list of properties into the square brackets after a name of entity:
	category[name version]
Due to last query you get a value of the property "name" and a value of the property "version" for
each object Category. You can use metasymbol & to definition self object. So the query
	catogory[& name version]
returns object Category, its name and its version for each object Category.

For definition some restrictions you should write HQL query like this:
	select c from Category as c where c.version=0 and c.name="some cat"
We offer to define restriction after name of entity and metasymbol # like this:
	category#(version=0 && name="some cat")
The complexity of restrictions is not limit:
	category#(version=0 || (name="some cat" && parentCategory.name="p_cat"))


## Notes about implementation of the YZ
This implementation of the YZ is based on Criteria API 2.0 
(it is part of the [JSR 220] (http://jcp.org/aboutJava/communityprocess/final/jsr220/index.html)) so you can use it in
case if you use some implementation of this API (for example, [OpenJPA] (http://openjpa.apache.org/), 
[Hibernate] (http://www.hibernate.org/), [ObjectBD] (http://www.objectdb.com/)).

## BNF 


