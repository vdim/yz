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

5. Create jar file
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
3. Using consice names of entities.
4. A default property.
5. Using the reduced notation for complex restrictions.

Let`s get an object model from the book ["Java persistence with Hibernate"] 
(http://www.amazon.com/Java-Persistence-Hibernate-Christian-Bauer/dp/1932394885). 
See source code in [http://downloads.jboss.org/hibernate/caveatemptor/] (http://downloads.jboss.org/hibernate/caveatemptor/).

## Notes about implementation of the YZ
This implementation of the YZ is based on Criteria API 2.0 so you can use it in
case if you use some implementation of this API (for example, [OpenJPA] (http://openjpa.apache.org/), 
[Hibernate] (http://www.hibernate.org/), [ObjectBD] (http://www.objectdb.com/)).

## BNF 


