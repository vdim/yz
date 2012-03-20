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

###Projection
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


### Filtering
In order to filter your collection due to some condition through some property 
you should specify your predicate after name of class before symbol "#" in parenthesis. You
should specify property in left part of expression then you should specify binary operation and
at last right part of expression

    string#(empty = false)
    => (["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"])

Of course it is more interesting the filtering self object, so YZ supports refering
to self object due to symbol "&":

    string#(& = "Bob")
    => (["Bob"])

YZ supports:
* equality (equals to = function of Clojure or .equals method of Java):

    string#(& = "Bob")
    => (["Bob"])

* >, <, >=, <= (rightly for numbers):

```clojure
(collq "integer#(& > 10)" [199 4 6 10 34])
([34] (199))
```

* regex (rightly for strings):

    string#(& ~ "^.a.*")
    => (["Marry"] ["David"])

* binary operation (&& and || and syntax sugar "and" and "or" correspondingly):

    string#(& = "Bob" || & = "Marry")
    => (["Bob"] ["Marry"]) 

* RCP (Reduced Complicate Predicates) allows to reduce your predicates for
same property:

    string#(& = ("Bob" || "Marry"))
    => (["Bob"] ["Marry"]) 

You can override operation:

    string#(& = ("Bob" || ~"^.a.*"))
    => (["Bob"] ["Marry"] ["David"]) 


