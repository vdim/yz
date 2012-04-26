# About

> "... it seems likely that the verbosity of a query language might obscure 
> important information or its syntax may emphasize unimportant query features 
> rather than important ones."
> -- from thesis ["Database Queries in Java"] (http://infoscience.epfl.ch/record/153803) by Christopher Ming-Yee Iu, page 61.


YZ[ˈiːzi] is simple (easy), laconic, string-based, object query language. 
At the moment there is implementation for Clojure/Java in Clojure.

#### What does YZ include?

* Selection:
<pre><code>entity</code></pre>

* Projection:
<pre><code>entity[prop1 prop2]</code></pre>

* Filtering:
<pre><code>entity#(prop="sv")</code></pre>

* Joining:
<pre><code>entity1 (entity2)</code></pre>

* Union:
<pre><code>entity1, entity2</code></pre>

* Sorting:
<pre><code>↓entity</code></pre>

* Calling an user function:
<pre><code>entity[prop1 @(f &)]</code></pre>

* Recursive queries:
<pre><code>entity[*parent]</code></pre>

* Removing duplicates:
<pre><code>¹entity</code></pre>

* Limits:
<pre><code>1-10:entity</code></pre>

* Default property:
<pre><code>entity#(.="sv")</code></pre>

#### What not?

* Definition.
* Creating.
* Updating.

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
* [Specification (grammatics, semantics, pragmatics)] (https://github.com/vdim/yz/blob/master/doc/SYNTAX.md)
* [Implementation (for Java/Clojure)] (https://github.com/vdim/yz/blob/master/doc/IMPLEMENTATION.md)
* [Our Environment] (https://github.com/vdim/yz/blob/master/doc/ENVIRONMENT.md)
* [Installation] (https://github.com/vdim/yz/blob/master/doc/INSTALLATION.md)
* [Usage] (https://github.com/vdim/yz/blob/master/doc/USAGE.md)
* [Testing and benchmarking] (https://github.com/vdim/yz/blob/master/doc/TESTING.md)

