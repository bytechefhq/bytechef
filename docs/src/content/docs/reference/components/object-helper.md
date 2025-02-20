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
Name: addValueByKey

Add value to the object by key if it exists. Otherwise, update the value

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Source object to be added or updated | true |
| key | Key | STRING | TEXT | Key of the value to be added or updated. | true |
| type | Type | STRING <details> <summary> Options </summary> ARRAY, BOOLEAN, DATE, DATE_TIME, INTEGER, NULL, NUMBER, OBJECT, STRING, TIME </details> | SELECT | Type of value to be added or updated. | true |
| value | Value | ARRAY <details> <summary> Items </summary> [] </details> | ARRAY_BUILDER | Value to be added or updated. | true |
| value | Value | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Value to be added or updated. | true |
| value | Value | DATE | DATE | Value to be added or updated. | true |
| value | Value | DATE_TIME | DATE_TIME | Value to be added or updated. | true |
| value | Value | INTEGER | INTEGER | Value to be added or updated. | true |
| value | Value | NULL | NULL | Value to be added or updated. | true |
| value | Value | NUMBER | NUMBER | Value to be added or updated. | true |
| value | Value | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Value to be added or updated. | true |
| value | Value | STRING | TEXT | Value to be added or updated. | true |
| value | Value | TIME | TIME | Value to be added or updated. | true |


#### JSON Example
```json
{
  "label" : "Add Value to the Object by Key",
  "name" : "addValueByKey",
  "parameters" : {
    "source" : { },
    "key" : "",
    "type" : "",
    "value" : "00:00:00"
  },
  "type" : "objectHelper/v1/addValueByKey"
}
```


### Add Key-Value Pairs to Object or Array
Name: addKeyValuePairs

Add values from list to object or array. If the source is object, the items in the list will be treated as Key-value pairs. If the value is array of objects, key-value pairs will be added to every object in the array.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| sourceType | Type of Initial Object | STRING <details> <summary> Options </summary> ARRAY, OBJECT </details> | SELECT | Type of initial object to be added or updated. | true |
| source | Source | ARRAY <details> <summary> Items </summary> [{}] </details> | ARRAY_BUILDER | Source object to be added or updated | true |
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Source object to be added or updated | true |
| value | Key-Value Pairs | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Key-Value pairs to be added or updated. | true |


#### JSON Example
```json
{
  "label" : "Add Key-Value Pairs to Object or Array",
  "name" : "addKeyValuePairs",
  "parameters" : {
    "sourceType" : "",
    "source" : { },
    "value" : { }
  },
  "type" : "objectHelper/v1/addKeyValuePairs"
}
```


### Contains
Name: contains

Checks if the given key exists in the given object.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| input | Input | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Object that you'd like to check. | true |
| key | Key | STRING | TEXT | Key to check for existence. | true |


#### Output



Type: BOOLEAN





#### JSON Example
```json
{
  "label" : "Contains",
  "name" : "contains",
  "parameters" : {
    "input" : { },
    "key" : ""
  },
  "type" : "objectHelper/v1/contains"
}
```


### Delete Key-Value Pair
Name: deleteKeyValuePair

Deletes a key-value pair in the given object by the specified key. Returns the modified object.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| input | Input | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The object from which to delete the key-value pair. | true |
| key | Key | STRING | TEXT | The key of the key-value pair to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Key-Value Pair",
  "name" : "deleteKeyValuePair",
  "parameters" : {
    "input" : { },
    "key" : ""
  },
  "type" : "objectHelper/v1/deleteKeyValuePair"
}
```


### Equals
Name: equals

Compares two objects and returns true if they are equal.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The source object to compare. | true |
| target | Target | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The target object to compare against. | true |


#### Output



Type: BOOLEAN





#### JSON Example
```json
{
  "label" : "Equals",
  "name" : "equals",
  "parameters" : {
    "source" : { },
    "target" : { }
  },
  "type" : "objectHelper/v1/equals"
}
```


### Merge Two Objects
Name: mergeTwoObjects

Merge two objects into one. If there is any property with the same name, the source value will be used.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The source object to merge. | true |
| target | Target | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The target object to merge into. | true |


#### JSON Example
```json
{
  "label" : "Merge Two Objects",
  "name" : "mergeTwoObjects",
  "parameters" : {
    "source" : { },
    "target" : { }
  },
  "type" : "objectHelper/v1/mergeTwoObjects"
}
```




