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
