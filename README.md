Implementation of the YZ query language specification.
## Installation

### Build from source code
1. Install [lein] (https://github.com/technomancy/leiningen) build tool.
2. Download source code from repository:
    $ git clone http://github.com/vdim/yz
    $ cd yz
3. Download dependencies:
    $ lein deps
4. Compile code:
    $ lein compile
5. Create jar file
    $ lein jar

### Install from repository
The YZ is distributed due to [clojars.org] (http://clojars.org), so
if you use some dependency manager (ivy, maven, leiningen and so on) you
can use it for getting the YZ. For example, you can write for ivy 
something like this:
	<pre><dependency org="ru.petrsu.nest" name="yz" rev="0.0.1-alpha1"/></pre>

## Usage
You can use YZ from your clojure code something like this:

	(ns some.ns
	  (:import (javax.persistence Persistence))
	  (:require [ru.petrsu.nest.yz.core :as c] [ru.petrsu.nest.yz.hb-utils :as hu]))

	(def emf (Persistence/createEntityManagerFactory "test-model"))
	(def mom (hu/gen-mom-from-metamodel emf))
	(def em (.createEntityManager emf))

	(c/pquery "text-of-query" mom em)

## YZ specification
