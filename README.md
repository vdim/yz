# About
YZ[ˈiːzi] is simple (easy) string-based query language for an object model (OM). 

#### What does YZ include?

* Selection:
<pre><code>entity</code></pre>

* Projection:
<pre><code>entity[prop1 prop2]</code></pre>

* Filtering:
<pre><code>entity#(prop1="sv")</code></pre>

* Joining:
<pre><code>entity1 (entity2)</code></pre>

* Union:
<pre><code>entity1, entity2</code></pre>

* Sorting:
<pre><code>↓entity</code></pre>

* Calling a user function:
<pre><code>entity[prop1 @(f &)]</code></pre>

* Recursive queries:
<pre><code>entity[*parent]</code></pre>

#### What not?

* Definitions (schema, new classes, instances of classes, functions and so on).
* Updating.
* Grouping.

## Motivation
We must allowed users of our project to getting any info about data domain. We did it. 
May be it will be usefull for you also.
 
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
YZ is distributed due to [clojars.org] (http://clojars.org), so
if you use some dependency manager ([ivy] (http://ant.apache.org/ivy/), 
[maven] (http://maven.apache.org/), [leiningen] (https://github.com/technomancy/leiningen) and so on) you
can use it for getting YZ. For example, you can write for ivy 
something like this:
	
	<dependency org="ru.petrsu.nest" name="yz" rev="0.0.1-alpha11"/>

Direct link: [http://clojars.org/ru.petrsu.nest/yz] (http://clojars.org/ru.petrsu.nest/yz)


## Restriction
Project is in alpha stage and it is just prototype, not for production use.

## Copyright

Copyright 2011 Vyacheslav Dimitrov, Petrozavodsk State University.

YZ is free software: you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License version 3
only, as published by the Free Software Foundation.

YZ is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with YZ.  If not, see [http://www.gnu.org/licenses/] (http://www.gnu.org/licenses/).
