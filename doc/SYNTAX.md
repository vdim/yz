# YZ
For demostration features of the YZ we will use some examples. First example is
collection with string values. Let's define this collection something like this:

```clojure
(def names ["Bob" "Alice" "" "Marry" "Kris" "David" "Alexander"])
```

In order to test and usage our examples you can use the collq function from the 
yz-factory namespace something like this:

```clojure
(collq "yourquery" names)
```

We will hold the following notation: first will be query and second - result of query after "=>":

    some-query
    => some-result

Note: for more details about usage YZ see [here] (https://github.com/vdim/yz/blob/master/doc/USAGE.md).


### Selection
In order to get data from your model you can specify class of objects which you
want to get. If your want all string you just write: 
    
    string

and this query returns all strings from our collection names:

    => (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])

### Projection
In case you want to get some property of object you must specify it in square brackets:

    string[empty]
    => ([false] [false] [true] [false] [false] [false] [false])

Note that access to properties depends on implementation of your ElementManager
(in our case it is element manager for simple collections).
For several properties you must enumerate it through whitespace: 

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

Of course it is more interesting the filtering self object (for strings or integers at least), 
so YZ supports refering to self object due to symbol "&":

    string#(& = "Bob")
    => (["Bob"])

YZ supports:

* equality (=, equals to = function of Clojure or .equals method of Java):
<pre><code>
    string#(& = "Bob")
    => (["Bob"])
</code></pre>

* identical (==, equals to identical? function of Clojure or == of Java):

```clojure
(collq "integer#(& = 1)" [1 2])
=> ([1])
(collq "integer#(& == 1)" [1 2])
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

* logical operation (&& and || and syntax sugar "and" and "or" correspondingly):
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
    string#(& = ("Bob" || ~"^.a.*"))
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
(collq "integer#(& = 1)" [1 2])
=> ([1])
(collq "integer#(& = -1)" [1 -1 2])
=> ([-1])
(collq "integer#(& = 1)" [1 2])
=> ([1])
(collq "long#(& = -1.1)" [-1 -2 -3 -1.1])
=> ([-1.1])
```

### Sorting
In order to sort your result you should use symbols "↑" and "↓" for
sorting by ascending and by descenting correspondingly:

    ↑string
    => ([""] ["Alexander"] ["Alice"] ["Bob"] ["David"] ["Kris"] ["Marry"])
    ↓string
    => (["Marry"] ["Kris"] ["David"] ["Bob"] ["Alice"] ["Alexander"] [""])

If you select properties and want to sort by it you should specify sorting for
properties:
    
    string[↑empty]
    => ([false] [false] [false] [false] [false] [false] [true])
    string[↓empty]
    => ([true] [false] [false] [false] [false] [false] [false])

If you want to sort by some property, but don't select it, you should specify
sorting into braces before name of your class:

    {↑empty}string
    => (["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"] [""])
    {↓empty}string
    => ([""] ["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"])

