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


## Selection
In order to get data from your model you can specify class of objects which you
want to get. If your want all string you just write: 
    
    string

and this query returns all strings from our collection names:

    => (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])

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


