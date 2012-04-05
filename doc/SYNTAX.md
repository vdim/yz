# YZ
## Simple collection
For demostration features of the YZ we will use some examples. First example is
collection with string values. Let's define this collection something like this 
(we use [Clojure] (http://clojure.org/) language):

```clojure
(def names ["Bob" "Alice" "" "Marry" "Kris" "David" "Alexander"])
```

In order to test and usage our examples you can use the collq function from the 
[yz-factory namespace] (https://github.com/vdim/yz/blob/master/src/ru/petrsu/nest/yz/yz_factory.clj) 
something like this:

```clojure
(collq "yourquery" names)
```

We will hold the following notation: first will be query and second - result of query after "=>":

    some-query
    => some-result

Note: for more details about usage existing implementation of the YZ 
see [here] (https://github.com/vdim/yz/blob/master/doc/USAGE.md).


### Selection
In order to get data from your model you can specify name of class of objects which you
want to get. If you want all strings you just query: 
    
    string

and this query returns all strings from collection names:

    => (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])

### Projection
In case you want to get some property of objects you must specify it in square brackets:

    string[empty]
    => ([false] [false] [true] [false] [false] [false] [false])

Note that access to properties depends on implementation of your ElementManager
(in our case it is element manager for simple collections).
For more details about current implementation of the YZ see [here] (https://github.com/vdim/yz/blob/master/doc/IMPLEMENTATION.md).

For several properties you must enumerate it through whitespace(s): 

    string[empty class]
    => ([false java.lang.String] [false java.lang.String] 
        [true java.lang.String] [false java.lang.String] 
        [false java.lang.String] [false java.lang.String] 
        [false java.lang.String])

In order to get self object in list with properties you can refer to self
object due to symbol "&":

    string[& empty]
    => (["Bob" false] ["Alice" false] ["" true] ["Marry" false] 
        ["Kris" false] ["David" false] ["Alexander" false]) 


### Filtering
In order to filter your collection due to some condition through some property 
you should specify your predicate after name of class before symbol "#" in parenthesis. You
should specify property in left part of expression then you should specify binary operation and
at last right part of expression:

    string#(empty = false)
    => (["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"])

If there is possibility for definition self object directly in text 
(it is rightly for string or numbers at list),
you can refer to self object due to symbol "&" in left side of predicate:

    string#(& = "Bob")
    => (["Bob"])

YZ supports the following operations:

* equality (=, equals to = function of Clojure or .equals method of Java):
<pre><code>
    string#(& = "Bob")
    => (["Bob"])
</code></pre>

* identical (==, equals to identical? function of Clojure or == operation of Java):

```clojure
(collq "long#(& = 1)" [1 2])
=> ([1])
(collq "long#(& == 1)" [1 2])
=> ([1])
(collq "integer#(& = 1)" [(Integer. 1) (Integer. 2)])
=> ([1])
(collq "integer#(& == 1)" [(Integer. 1) (Integer. 2)])
=> ()
```

* >, <, >=, <= (rightly for numbers):

```clojure
(collq "integer#(& > 10)" [199 4 6 10 34])
=> ([34] [199])
```

* negation (!= and syntax sugar not=):
<pre><code>
    string#(& != "Bob")
    => (["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])
</code></pre>

* regex (rightly for strings):
<pre><code>
    string#(& ~ "^.a.*")
    => (["Marry"] ["David"])
</code></pre>

* logical operation (&& and || and syntax sugar "and" and "or" respectively):
<pre><code>
    string#(& = "Bob" || & = "Marry")
    => (["Bob"] ["Marry"]) 
</code></pre>

* RCP (Reduced Complicate Predicates, this technique allows to reduce text of 
your predicates which is adjusted to same property):
<pre><code>
    string#(& = ("Bob" || "Marry"))
    => (["Bob"] ["Marry"]) 
</code></pre>

* overriding binary operation in case RCP is used:
<pre><code>
    string#(& = ("Bob" || "Mike" || ~"^.a.*"))
    => (["Bob"] ["Marry"] ["David"]) 
</code></pre>


The right side of predicate may contains:

* Strings
<pre><code>
    string#(& = "Bob")
    => (["Bob"])
</code></pre>

* Numbers (integer, real, negative):

```clojure
(collq "long#(& = 1)" [1 2])
=> ([1])
(collq "long#(& = -1)" [1 -1 2])
=> ([-1])
(collq "long#(& = 1)" [1 2])
=> ([1])
(collq "long#(& = -1.1)" [-1 -2 -3 -1.1])
=> ([-1.1])
```

* Keywords (nil, true, false):

<pre><code>
    string#(empty = false)
    => (["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"])
    string#(empty = true)
    => ([""])
    string#(empty = nil)
    => (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])
</code></pre>

* Subquery

### Sorting
In order to sort your result you should use symbols "↑" and "↓" for
sorting by ascending and by descenting respectively:

    ↑string
    => ([""] ["Alexander"] ["Alice"] ["Bob"] ["David"] ["Kris"] ["Marry"])
    ↓string
    => (["Marry"] ["Kris"] ["David"] ["Bob"] ["Alice"] ["Alexander"] [""])

If you select property and want to sort by it you should specify sorting for
properties:
    
    string[↑empty]
    => ([false] [false] [false] [false] [false] [false] [true])
    string[↓empty]
    => ([true] [false] [false] [false] [false] [false] [false])

If you select several properties and specify sorting for each property then
result will be sorted by first property firstly, second property secondly and so on:


    string[↓empty ↓&]
    => ([true ""] [false "Marry"] [false "Kris"] [false "David"] 
        [false "Bob"] [false "Alice"] [false "Alexander"])
    string[↓& ↓empty]
    => (["Marry" false] ["Kris" false] ["David" false] ["Bob" false] 
        ["Alice" false] ["Alexander" false] ["" true])


If you want to sort by some property, but don't select it, you should specify
sorting into braces before name of your class:

    {↑empty}string
    => (["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"] [""])
    {↓empty}string
    => ([""] ["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"])

You can specify several properties for sorting:

    {↓& ↓empty}string
    => (["Marry"] ["Kris"] ["David"] ["Bob"] ["Alice"] ["Alexander"] [""])
    {↑& ↓empty}string
    => ([""] ["Alexander"] ["Alice"] ["Bob"] ["David"] ["Kris"] ["Marry"])
    {↓empty ↑&}string
    => ([""] ["Alexander"] ["Alice"] ["Bob"] ["David"] ["Kris"] ["Marry"])
    {↓empty ↓&}string
    => ([""] ["Marry"] ["Kris"] ["David"] ["Bob"] ["Alice"] ["Alexander"])    

Also there are printed version for "↓" and "↑" symbols: "d:" and "a:"
correspondingly:

    a:string
    => ([""] ["Alexander"] ["Alice"] ["Bob"] ["David"] ["Kris"] ["Marry"])
    d:string
    => (["Marry"] ["Kris"] ["David"] ["Bob"] ["Alice"] ["Alexander"] [""])

Note that sorting requires that your objects (or property) must implements Comparable interface.


### Calling user function
YZ allows to call user function:

    @(count `string')
    => ([7])

Notes:

* Languages and calling mechanism are defined by concrete implementation of the YZ.
* Parameters:
    * result of query
    * strings
    * numbers
    * result of calling another function


## More complex model
So far we test simple flat collection. Let's consider more complex model 
(we choose classic example about university):
<img src="https://github.com/vdim/yz/raw/master/doc/uni_model.png" alt="UML class diagram for university model"/>

We implement it (see source code [here](https://github.com/vdim/yz/blob/master/test/src/university/model)) 
and create some [example] (https://github.com/vdim/yz/blob/master/test/src/ru/petrsu/nest/yz/queries/uni_bd.clj)
which is represented on figure below in UML object diagram notation:
<img src="https://github.com/vdim/yz/raw/master/doc/uni_model_object.png" 
alt="Example of university model in UML object diagram notation"/>

In order to use this university model and next queries you can follow 
[this] (https://github.com/vdim/yz/blob/master/doc/TESTINGUNIMODEL.md) instruction.


### Joining
So let's start. In order to select linked objects you should use brackets. For example, if you want to
get all courses and its faculty you should query:

    course (faculty)

This query returns the following result:

    ([#<Course Algebra> #<Faculty Marry>]
     [#<Course Geometry> #<Faculty David>]
     [#<Course Russian> #<Faculty Brian>]
     [#<Course German> #<Faculty Brian>])}
 
Note that so far we work with flat collection and get result as flat rows. But now
you can get a hierarchical result:

    [[#<Course Algebra>
     [[#<Faculty Marry> []]]
     #<Course Geometry>
     [[#<Faculty David> []]]
     #<Course Russian>
     [[#<Faculty Brian> []]]
     #<Course German>
     [[#<Faculty Brian> []]]]],

