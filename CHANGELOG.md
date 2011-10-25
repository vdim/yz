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
