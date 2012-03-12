# About
YZ[ˈiːzi] is simple (easy) string-based query language for a java object model. 

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

#### Supported backends:

* [Collections] (http://docs.oracle.com/javase/tutorial/collections/).
* [JPA] (http://jcp.org/aboutJava/communityprocess/final/jsr317/index.html).

## Restriction
Project is in alpha stage and it is just prototype, not for production use.

## Copyright

Copyright 2011-2012 Vyacheslav Dimitrov, [Petrozavodsk State University] (http://petrsu.ru/Structure/structure_e.html).

YZ is free software: you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License version 3
only, as published by the Free Software Foundation.

YZ is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with YZ.  If not, see [http://www.gnu.org/licenses/] (http://www.gnu.org/licenses/).

## Documentation
* [Installation] (https://github.com/vdim/yz/blob/master/doc/INSTALLATION.md)
* [Usage] (https://github.com/vdim/yz/blob/master/doc/USAGE.md)
* [Testing and benchmarking] (https://github.com/vdim/yz/blob/master/doc/TESTING.md)

