---
title: "Object Helper"
description: "Object Helper allows you to do various operations on objects."
---
## Reference
<hr />

Object Helper allows you to do various operations on objects.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Add Value to the Object by Key
Add value to the object by key if it exists. Otherwise, update the value

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Source | {} | OBJECT_BUILDER  |  Source object to be added or updated  |
| Key | STRING | TEXT  |  Key of the value to be added or updated.  |
| Value Type | INTEGER | SELECT  |  Type of value to be added or updated.  |
| Value | [] | ARRAY_BUILDER  |  Value to be added or updated.  |
| Value | BOOLEAN | SELECT  |  Value to be added or updated.  |
| Value | DATE | DATE  |  Value to be added or updated.  |
| Value | DATE_TIME | DATE_TIME  |  Value to be added or updated.  |
| Value | INTEGER | INTEGER  |  Value to be added or updated.  |
| Value | NULL | NULL  |  Value to be added or updated.  |
| Value | NUMBER | NUMBER  |  Value to be added or updated.  |
| Value | {} | OBJECT_BUILDER  |  Value to be added or updated.  |
| Value | STRING | TEXT  |  Value to be added or updated.  |
| Value | TIME | TIME  |  Value to be added or updated.  |




### Add Key-Value Pairs to Object or Array
Add values from list to object or array. If the source is object, the items in the list will be treated as Key-value pairs. If the value is array of objects, key-value pairs will be added to every object in the array.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Type of Initial Object | INTEGER | SELECT  |  Type of initial object to be added or updated.  |
| Source | [{}] | ARRAY_BUILDER  |  Source object to be added or updated  |
| Source | {} | OBJECT_BUILDER  |  Source object to be added or updated  |
| Key-Value Pairs | {} | OBJECT_BUILDER  |  Key-Value pairs to be added or updated.  |




### Contains
Checks if the given key exists in the given object.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Input | {} | OBJECT_BUILDER  |  Object that you'd like to check.  |
| Key | STRING | TEXT  |  Key to check for existence.  |


### Output



Type: BOOLEAN







### Delete Key-Value Pair
Deletes a key-value pair in the given object by the specified key. Returns the modified object.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Input | {} | OBJECT_BUILDER  |  The object from which to delete the key-value pair.  |
| Key | STRING | TEXT  |  The key of the key-value pair to delete.  |




### Equals
Compares two objects and returns true if they are equal.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Source | {} | OBJECT_BUILDER  |  The source object to compare.  |
| Target | {} | OBJECT_BUILDER  |  The target object to compare against.  |


### Output



Type: BOOLEAN







