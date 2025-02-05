---
title: "Object Helper"
description: "Object Helper allows you to do various operations on objects."
---

Object Helper allows you to do various operations on objects.


Categories: helpers


Type: objectHelper/v1

<hr />




## Actions


### Add Value to the Object by Key
Add value to the object by key if it exists. Otherwise, update the value

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| source | Source | {} | OBJECT_BUILDER  |  Source object to be added or updated  |  true  |
| key | Key | STRING | TEXT  |  Key of the value to be added or updated.  |  true  |
| type | Type | STRING | SELECT  |  Type of value to be added or updated.  |  true  |
| value | Value | [] | ARRAY_BUILDER  |  Value to be added or updated.  |  true  |
| value | Value | BOOLEAN | SELECT  |  Value to be added or updated.  |  true  |
| value | Value | DATE | DATE  |  Value to be added or updated.  |  true  |
| value | Value | DATE_TIME | DATE_TIME  |  Value to be added or updated.  |  true  |
| value | Value | INTEGER | INTEGER  |  Value to be added or updated.  |  true  |
| value | Value | NULL | NULL  |  Value to be added or updated.  |  true  |
| value | Value | NUMBER | NUMBER  |  Value to be added or updated.  |  true  |
| value | Value | {} | OBJECT_BUILDER  |  Value to be added or updated.  |  true  |
| value | Value | STRING | TEXT  |  Value to be added or updated.  |  true  |
| value | Value | TIME | TIME  |  Value to be added or updated.  |  true  |




### Add Key-Value Pairs to Object or Array
Add values from list to object or array. If the source is object, the items in the list will be treated as Key-value pairs. If the value is array of objects, key-value pairs will be added to every object in the array.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| sourceType | Type of Initial Object | STRING | SELECT  |  Type of initial object to be added or updated.  |  true  |
| source | Source | [{}] | ARRAY_BUILDER  |  Source object to be added or updated  |  true  |
| source | Source | {} | OBJECT_BUILDER  |  Source object to be added or updated  |  true  |
| value | Key-Value Pairs | {} | OBJECT_BUILDER  |  Key-Value pairs to be added or updated.  |  true  |




### Contains
Checks if the given key exists in the given object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| input | Input | {} | OBJECT_BUILDER  |  Object that you'd like to check.  |  true  |
| key | Key | STRING | TEXT  |  Key to check for existence.  |  true  |


#### Output



Type: BOOLEAN







### Delete Key-Value Pair
Deletes a key-value pair in the given object by the specified key. Returns the modified object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| input | Input | {} | OBJECT_BUILDER  |  The object from which to delete the key-value pair.  |  true  |
| key | Key | STRING | TEXT  |  The key of the key-value pair to delete.  |  true  |




### Equals
Compares two objects and returns true if they are equal.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| source | Source | {} | OBJECT_BUILDER  |  The source object to compare.  |  true  |
| target | Target | {} | OBJECT_BUILDER  |  The target object to compare against.  |  true  |


#### Output



Type: BOOLEAN







### Merge Two Objects
Merge two objects into one. If there is any property with the same name, the source value will be used.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| source | Source | {} | OBJECT_BUILDER  |  The source object to merge.  |  true  |
| target | Target | {} | OBJECT_BUILDER  |  The target object to merge into.  |  true  |






