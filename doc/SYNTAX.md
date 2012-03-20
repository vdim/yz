# YZ
For demostration features of the YZ we will use some examples. First example is
collection with string values. Let's define this collection something like this:

```clojure
(def names ["Bob" "Alice" "" "Marry" "Kris" "David" "Alexander"])
```

For testing and usage our examples you can use the collq function from the 
yz-factory namespace something like this:

```clojure
(collq "yourquery" names)
```

You will hold the following notation: first will be query and second after "=>"- result of query:

    some-query
    => some-result

Note: for more details about usage YZ see [here] (https://github.com/vdim/yz/blob/master/doc/USAGE.md).


## Selection
For simple getting data from your model you can specify class of objects which you
want to get. This query returns all strings from our collection names:
    
    string
    => (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])

In case you want to get some property of object you must specify it in square brackets:

    string[empty]
    => ([false] [false] [true] [false] [false] [false] [false])

Notes that access to properties is depends on implementation of your ElementManager.
For several properties you must enumerate it through whitespace: 

    string[empty class]
    => ([false java.lang.String] [false java.lang.String] 
	[true java.lang.String] [false java.lang.String] 
	[false java.lang.String] [false java.lang.String] [false java.lang.String])


<--
## More complex example
Let's consider more complex example for demonstration another feaures of the YZ.
Second example is some data model which is represented in figure: -->
