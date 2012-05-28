## 0.0.1-alpha14 / 28.05.2012
 
* Added support >, <, >=, <= operation for all comparable objects.
* Added checking property during parsing in case mom is defined.
* Added throwable object to the Result record.
* Added support negative predicates for all supported signs: floor#(number !> 0)

## 0.0.1-alpha13 / 27.04.2012

* Added throwing exception during parsing in case object is not comparable, but sorting is defined.
* Renamed class NotDefinedDP to NotDefinedDPException.
* New philosophy of the collq function.
* Added support limits: 2-10:string
* Added support removing duplicates: ¹string

## 0.0.1-alpha12 / 26.03.2012

* Added support refering to self objects into left side of predicates: string#(& = "some str")
* Added support subquries in the right part of predicates.
* API: replaced ElementManager`s getClasses method by the getMom method.
* API: replaced ElementManagerFactory by YZFactory which includes methods for getting different ElementManagers and instance QueryYZs.
* Added support queries through Java collection (CollectionElementManager).
* Added support parameterized queries: floor#(number=$1)
* Added support ALL modificator for collections into predicates: building#(∀floor.number=1)
* Added support definition an exact type of class: ni^
* Added identity(==) binary operation.
* Added support default properties something like this: floor#(.=1)

## 0.0.1-alpha11 / 25.11.2011

* Added support regular expressions into predicates: building#(name~"^Main.*ding$")
* Fixed problem with performance (due to removing the remove-duplicate function).
* Added support sorting besides properties which are selected: {↓name ↓description ↓@(count `device')}room
* Added typing syntax for sorting: a:room[name d:number]
* Fixed problem with queries like this: @(inc 1), room
* Added support default properties into predicates: building#(floor.=1)
* Added new method about ElementManager interface.

## 0.0.1-alpha10 / 18.11.2011

* Added support processing property from the MOM.
* Added getSingleResult method to the QueryYZ wrapper.
* Added factory (ElementManagerFactory) with different implementation of the ElementManager.
* Added support the sorting of result`s query.

## 0.0.1-alpha9 / 05.11.2011

* Fixed problem with inheritance from interface.
* A little bit improving performance.
* Added support default property (for list with properties and parameter in function).
* Removed duplicate rows from value of :rows key from result of pquery function.
* Added getStructuredResult to Java`s YZ wrapper.

## 0.0.1-alpha8 / 28.10.2011

* Fixed problems with interfaces and properties.

## 0.0.1-alpha7 / 28.10.2011

* Fixed bug with a link to array.

## 0.0.1-alpha6 / 28.10.2011

* Fixed problem with nil into the restriction: building#(description = nil)
* Added some new built-in functions (ip, mac, ip2b, name2ip).
* Fixed some minor bugs.

## 0.0.1-alpha5 / 27.10.2011

* Fixed bug with searching property of object.

## 0.0.1-alpha4 / 27.10.2011

* Improved support functions.
* Replaced reflection with field by bean.

## 0.0.1-alpha3 / 25.10.2011

* Added throwing in case a remainder of the parsing is not nil.
* Fixed bug with multiple or/and into reduced complex predicates: floor#(number=(1 || 2 || 3))
* Predicates for root element go to the Criteria API (for improving performance). True for users of Criteria API.
* Fixed problem with referencing to property from property: compositeou#(parent.id = 4)
* Fixed infinite loop for queries without links.
* YZ without Criteria API (experimental).

## 0.0.1-alpha2 / 10.10.2011

* Added a wrapper for Java user.
* Added functions for saving/loading the MOM to/from file.
* Added syntactic sugar for "and" and "or" operations ("&&" and "||" respectively).
* Added support "!=" binary operation into restrictions.
* Added support "nil" restrictions. Something like this: people#(name != nil)

## 0.0.1-alpha1 / 06.10.2011

* First alpha release is out!
