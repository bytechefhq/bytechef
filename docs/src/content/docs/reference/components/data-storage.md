---
title: "Data Storage"
description: "Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value."
---
## Reference
<hr />

Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value.

Categories: [HELPERS]

Version: 1

<hr />






## Actions


### Append Value to List
Append value to the end of a list. If the list does not exist, it will be created.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Scope | STRING | SELECT  |
| Key | STRING | TEXT  |
| Type | INTEGER | SELECT  |
| Value | ARRAY | ARRAY_BUILDER  |
| Value | BOOLEAN | SELECT  |
| Value | DATE | DATE  |
| Value | DATE_TIME | DATE_TIME  |
| Value | INTEGER | INTEGER  |
| Value | NULL | NULL  |
| Value | NUMBER | NUMBER  |
| Value | OBJECT | OBJECT_BUILDER  |
| Value | STRING | TEXT  |
| Value | TIME | TIME  |
| Append a list as a single item | BOOLEAN | SELECT  |




### Atomic Increment
The numeric value can be incremented atomically, and the action can be used concurrently from multiple executions.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |
| Value to add | INTEGER | INTEGER  |


### Output



Type: INTEGER

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Await Get Value
Wait for a value under a specified key, until it's available.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |
| Type | INTEGER | SELECT  |
| Default value | ARRAY | ARRAY_BUILDER  |
| Default value | BOOLEAN | SELECT  |
| Default value | DATE | DATE  |
| Default value | DATE_TIME | DATE_TIME  |
| Default value | INTEGER | INTEGER  |
| Default value | NULL | NULL  |
| Default value | NUMBER | NUMBER  |
| Default value | OBJECT | OBJECT_BUILDER  |
| Default value | STRING | TEXT  |
| Default value | TIME | TIME  |
| Timeout (1 to 300 sec) | INTEGER | INTEGER  |




### Delete Value
Remove a value associated with a key in the specified scope.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |




### Delete Value from List
Delete a value from the given index in a list.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |
| Index | INTEGER | INTEGER  |




### Get All Entries(Keys and Values)
Retrieve all the currently existing keys from storage, along with their values within the provided scope.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Scope | STRING | SELECT  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Get Value
Retrieve a previously assigned value within the specified scope using its corresponding key.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |
| Type | INTEGER | SELECT  |
| Default value | ARRAY | ARRAY_BUILDER  |
| Default value | BOOLEAN | SELECT  |
| Default value | DATE | DATE  |
| Default value | DATE_TIME | DATE_TIME  |
| Default value | INTEGER | INTEGER  |
| Default value | NULL | NULL  |
| Default value | NUMBER | NUMBER  |
| Default value | OBJECT | OBJECT_BUILDER  |
| Default value | STRING | TEXT  |
| Default value | TIME | TIME  |




### Set Value
Set a value under a key, in the specified scope.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |
| Type | INTEGER | SELECT  |
| Value | ARRAY | ARRAY_BUILDER  |
| Value | BOOLEAN | SELECT  |
| Value | DATE | DATE  |
| Value | DATE_TIME | DATE_TIME  |
| Value | INTEGER | INTEGER  |
| Value | NULL | NULL  |
| Value | NUMBER | NUMBER  |
| Value | OBJECT | OBJECT_BUILDER  |
| Value | STRING | TEXT  |
| Value | TIME | TIME  |




### Set Value in List
Set value under a specified index in a list.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Scope | STRING | SELECT  |
| Index | INTEGER | INTEGER  |
| Type | INTEGER | SELECT  |
| Value | ARRAY | ARRAY_BUILDER  |
| Value | BOOLEAN | SELECT  |
| Value | DATE | DATE  |
| Value | DATE_TIME | DATE_TIME  |
| Value | INTEGER | INTEGER  |
| Value | NULL | NULL  |
| Value | NUMBER | NUMBER  |
| Value | OBJECT | OBJECT_BUILDER  |
| Value | STRING | TEXT  |
| Value | TIME | TIME  |




