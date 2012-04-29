# YZ

* <a href="#simple_collection">Simple collection</a>
    * <a href="#selection">Selection</a>
    * <a href="#projection">Projection</a>
    * <a href="#filtering">Filtering</a>
    * <a href="#sorting">Sorting</a>
    * <a href="#calling_function">Calling user function</a>
    * <a href="#removing_dupls">Removing duplicates</a>
* <a href="#complex_model">More complex model</a>
    * <a href="#joining">Joining</a>
    * <a href="#joining_not_result">Joining without including to result</a>
    * <a href="#union">Union</a>
    * <a href="#default_property">Default property</a>
    * <a href="#subquery">Subquery in right side of predicates</a>
* <a href="#table_typed_notyped">Table with typed and not typed symbols</a></li>

<a name="simple_collection"></a>
## Simple collection 
For demostration features of the YZ we will use some examples. First example is
collection with string values. Let's define this collection something like this:

    ["Bob" "Alice" "" "Marry" "Kris" "David" "Alexander"]

In order to test and usage our examples you can use existing implementation of the YZ for
Clojure/Java object model. Follow to [this] (https://github.com/vdim/yz/blob/master/doc/TESTINGCOLLQ.md) 
instruction for using queries for simple collection.

We will hold the following notation: first will be query and second - result of query after "=>":

    some-query
    => some-result

In some cases it may be convenient to demonstrate feature of the YZ using other collection 
(for example collection with numbers) rather specified above collection with string. 
So the collq function from the current implementation of the YZ is used. 
The collq function takes two parameters (query and collection) and returns
result of query. Example:

```clojure
(collq "some-query" some-collection)
=> some-result
```

<a name="selection"></a>
### Selection 
In order to get data from your model you can specify name of class of objects which you
want to get. If you want all strings you just query: 
    
    string

and this query returns all strings from collection names:

    => (["Bob"] ["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])

<a name="projection"></a>
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


<a name="filtering"></a>
### Filtering
In order to filter your collection due to some condition(s) through some property 
you should specify your predicate after name of class before symbol "#" in parenthesis. You
should specify property in left part of expression then you should specify binary operation and
at last right part of expression:

    string#(empty = false)
    => (["Bob"] ["Alice"] ["Marry"] ["Kris"] ["David"] ["Alexander"])

If there is possibility for definition self object directly in text 
(it is rightly for string or numbers at least),
you can refer to self object due to symbol "&" in left side of predicate:

    string#(& = "Bob")
    => (["Bob"])

YZ supports the following operations:

* equality (=, equals to = function of Clojure or .equals method of Java):
<pre><code>
    string#(& = "Bob")
    => (["Bob"])
</code></pre>

* identical ("=="):

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
(collq "long#(& > 10)" [199 4 6 10 34])
=> ([34] [199])
```

* regex (rightly for strings):
<pre><code>
    string#(& ~ "^.a.*")
    => (["Marry"] ["David"])
</code></pre>

* negation (in order to get opposite result for a sign you should specify the symbol "!" before this sign):
<pre><code>
    string#(& != "Bob")
    => (["Alice"] [""] ["Marry"] ["Kris"] ["David"] ["Alexander"])
</code></pre>

* logical operation (&& and || and syntax sugar "and" and "or" respectively):
<pre><code>
    string#(& = "Bob" || & = "Marry")
    => (["Bob"] ["Marry"]) 
</code></pre>

```clojure
(collq "long#(& !> 10)" [199 4 6 10 34])
=> ([4] [6] [10])
```

* RCP (Reduced Complicate Predicates, this technique allows to reduce text of 
your predicates which are adjusted to same property):
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

<a name="sorting"></a>
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


<a name="calling_function"></a>
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


<a name="removing_dupls"></a>
### Removing duplicates:
Use symbol "¹" or "u:" for removing duplicates from result of query:

```clojure
(collq "long" [1 2 1])
=> ([1 2 1])
(collq "¹long" [1 2 1])
=> ([1 2])
(collq "u:long" [1 2 1])
=> ([1 2])
```

<a name="complex_model"></a>
## More complex model
So far we test simple flat collection. Let's consider more complex model 
(we choose classic example about university):
<br />
<img src="https://github.com/vdim/yz/raw/master/doc/uni_model.png" 
alt="UML class diagram for university model" title="UML class diagram for university model"/>

We implement it (see source code [here](https://github.com/vdim/yz/blob/master/test/src/university/model)) 
and create some [example] (https://github.com/vdim/yz/blob/master/test/src/ru/petrsu/nest/yz/queries/uni_bd.clj)
which is represented on figure below in UML object diagram notation:
<img src="https://github.com/vdim/yz/raw/master/doc/uni_model_object.png" 
alt="Example of university model in UML object diagram notation"
title="Example of university model in UML object diagram notation"/>

In order to use this university model and next queries you can follow to 
[this] (https://github.com/vdim/yz/blob/master/doc/TESTINGUNIMODEL.md) instruction.


<a name="joining"></a>
### Joining
In order to select linked objects you should use round brackets. For example, if you want to
get all courses and its faculty you should query:

    course (faculty)

This query returns the following result:

    => ([#<Course Algebra> #<Faculty Marry>]
        [#<Course Geometry> #<Faculty David>]
        [#<Course Russian> #<Faculty Brian>]
        [#<Course German> #<Faculty Brian>])}
 
Note that so far we work with flat collection and get result as flat rows. But now
a hierarchical result may be interesting:

    => [[#<Course Algebra>
        [[#<Faculty Marry> []]]
        #<Course Geometry>
        [[#<Faculty David> []]]
        #<Course Russian>
        [[#<Faculty Brian> []]]
        #<Course German>
        [[#<Faculty Brian> []]]]],

In case you define the following query

    course (faculty (student))

you get all courses, its faculty and for each faculty his/her students:

    [[#<Course Algebra>
     [[#<Faculty Marry>
       [[#<Student John> [] #<Student Alexander> [] #<Student Nik> []]]]]
     #<Course Geometry>
     [[#<Faculty David> [[#<Student John> [] #<Student Nik> []]]]]
     #<Course Russian>
     [[#<Faculty Brian>
       [[#<Student John>
         []
         #<Student Bob>
         []
         #<Student John>
         []
         #<Student Bob>
         []]]]]
     #<Course German>
     [[#<Faculty Brian>
       [[#<Student John>
         []
         #<Student Bob>
         []
         #<Student John>
         []
         #<Student Bob>
         []]]]]]]

    ([#<Course Algebra> #<Faculty Marry> #<Student John>]
     [#<Course Algebra> #<Faculty Marry> #<Student Alexander>]
     [#<Course Algebra> #<Faculty Marry> #<Student Nik>]
     [#<Course Geometry> #<Faculty David> #<Student John>]
     [#<Course Geometry> #<Faculty David> #<Student Nik>]
     [#<Course Russian> #<Faculty Brian> #<Student John>]
     [#<Course Russian> #<Faculty Brian> #<Student Bob>]
     [#<Course Russian> #<Faculty Brian> #<Student John>]
     [#<Course Russian> #<Faculty Brian> #<Student Bob>]
     [#<Course German> #<Faculty Brian> #<Student John>]
     [#<Course German> #<Faculty Brian> #<Student Bob>]
     [#<Course German> #<Faculty Brian> #<Student John>]
     [#<Course German> #<Faculty Brian> #<Student Bob>])


In case you want to get all courses and its faculty and students, you can define the
following query:

    course (faculty, student)
    => [[#<Course Algebra>
         [[#<Faculty Marry> []]
          [#<Student John> [] #<Student Alexander> [] #<Student Nik> []]]
         #<Course Geometry>
         [[#<Faculty David> []] [#<Student John> [] #<Student Nik> []]]
         #<Course Russian>
         [[#<Faculty Brian> []] [#<Student John> [] #<Student Bob> []]]
         #<Course German>
         [[#<Faculty Brian> []] [#<Student John> [] #<Student Bob> []]]]]

    => ([#<Course Algebra> #<Faculty Marry>]
        [#<Course Algebra> #<Student John>]
        [#<Course Algebra> #<Student Alexander>]
        [#<Course Algebra> #<Student Nik>]
        [#<Course Geometry> #<Faculty David>]
        [#<Course Geometry> #<Student John>]
        [#<Course Geometry> #<Student Nik>]
        [#<Course Russian> #<Faculty Brian>]
        [#<Course Russian> #<Student John>]
        [#<Course Russian> #<Student Bob>]
        [#<Course German> #<Faculty Brian>]
        [#<Course German> #<Student John>]
        [#<Course German> #<Student Bob>])

You can apply for each entity in query action which was describe above 
(sorting, filtering, projection, removing duplicates). Note that this operations are
applied to current subset of objects, not for all. For example query
    
    course (faculty#(name="Brian"))

returns <b>all</b> courses and its faculties in case he/she has name "Brian":

    => ([#<Course Algebra>]
        [#<Course Geometry>]
        [#<Course Russian> #<Faculty Brian>]
	[#<Course German> #<Faculty Brian>])

In case you want to get courses which is taught with Brian, you can try:

    course#(faculty.name="Brian")
    => ([#<Course Russian>] [#<Course German>])


<a name="joining_not_result"></a>
### Joining without including to result
Round brackets allow user to join an entity to an another entity and include objects of both
entities to result. In case you want to join entities, but not include one of it
you can use "." symbol. For example, query:
    
    course.student
    =>  ([#<Student Alexander>]
         [#<Student Nik>]
         [#<Student John>]
         [#<Student Nik>]
         [#<Student John>]
         [#<Student Bob>]
         [#<Student John>]
         [#<Student Bob>]
         [#<Student John>])

Some notes:

* This feature may be usefull for definition path between entities which is not
used by default. Let's imagine abstract object model something like represented
on figure below:

<img src="https://github.com/vdim/yz/raw/master/doc/abstract-om.png" 
alt="Some abstract object model" title="Some abstract object model"/>

We want to select objects of the Entity1 and its objects of the Entity2 throught Entity3 and Entity4 (green path),
but query
    
    entity1 (entity2)

may return results due to wrong red path (in fact it depends on implementation of the YZ, but let's suppose that
our implementation of the YZ returns first shortest path). In order to solve this problem you can query

    entity1 (entity3.entity2)

which returns result due to right green path (first, YZ try to find path between classes Entity1 and Entity3 and
then between classes Entity3 and Entity2).

* "student" in above query is not property, but it is entity for which YZ tries to find path. 

If searching fails then YZ tries to recognize property and extract its value:

    course.students
    => ([#<HashSet [Alexander, Nik, John]>]
        [#<HashSet [Nik, John]>]
        [#<HashSet [Bob, John]>]
        [#<HashSet [Bob, John]>])


<a name="union"></a>
### Union
In case you want to get results of several queries in single query, you
can use "," for union results

    course, faculty
    => ([#<Course Algebra>]
        [#<Course Geometry>]
        [#<Course Russian>]
        [#<Course German>]
        [#<Faculty Marry>]
        [#<Faculty David>]
        [#<Faculty Brian>]
        [#<Faculty Bob>])

    course, @(count `course')
    => ([#<Course Algebra>]
        [#<Course Geometry>]
        [#<Course Russian>]
        [#<Course German>]
        [4])

    @(count `course'), @(count `faculty'), @(count `student')
    => ([4] [4] [4])


<a name="default_property"></a>
### Default property
Object may have property which is distinctive feature for this object or this property is often used. 
For example, for class Course such property may be property "title". So the YZ supports for such
properties syntax sugar in where and projection clauses: 

    course#(.="Russian")
    => ([#<Course Russian>])

    course[&.]
    => (["Algebra"] ["Geometry"] ["Russian"] ["German"])


In case default property is not specified then the YZ throws exception.


<a name="subquery"></a>
### Subquery in right side of predicates
You can use YZ query in the right side of predicate of another YZ query.
For example you want to get students which have same name of his/her faculties:

    student#(name=faculty.name)
    => ()

Result is empty, because there is not such student. But if you want find 
students which have same name from all university faculties, you can try:

    student#(name=Ŷfaculty.name)
    => ([#<Student Bob>])

Modificator Ŷ denotes all existing faculties (not only linked with current student). As you can note,
subquery returns collection and two previous queries check collection by "at least one" option. In case
you want check collection by "all" option, you can use modificator ∀. Examples:

    student#(name != Ŷfaculty.name)
    => ([#<Student Alexander>] [#<Student Nik>] [#<Student John>] [#<Student Bob>])
    student#(name != Ŷ∀faculty.name)
    => ([#<Student Alexander>] [#<Student Nik>] [#<Student John>])
    student#(name != ∀faculty.name)
    => ([#<Student Alexander>] [#<Student Nik>] [#<Student John>] [#<Student Bob>])


<a name="table_typed_notyped"></a>
## Table with typed and not typed symbols

<table>
    <tr>
        <td>Not typed</td>
        <td>Typed</td>
	<td>Note</td>
    </tr>
    <tr>
        <td>↑</td>
        <td>a:</td>
	<td>Sort by ascending.</td>
    </tr>
    <tr>
        <td>↓</td>
        <td>d:</td>
	<td>Sort by descending.</td>
    </tr>
    <tr>
        <td>∀</td>
        <td>all:</td>
	<td>Check all elements from collection.</td>
    </tr>
    <tr>
        <td>Ŷ</td>
        <td>A:</td>
	<td>Select all elements (not only linked with current object).</td>
    </tr>
    <tr>
        <td>¹</td>
        <td>u:</td>
	<td>Remove duplicates.</td>
    </tr>
</table>
