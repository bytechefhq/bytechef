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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Scope | STRING | SELECT  |  The namespace for appending a value.  |
| Key | STRING | TEXT  |  The identifier of a list must be unique within the chosen scope, or a new value will overwrite the existing one.  |
| Type | INTEGER | SELECT  |  The value type.  |
| Value | [] | ARRAY_BUILDER  |  The value to set under given key.  |
| Value | BOOLEAN | SELECT  |  The value to set under given key.  |
| Value | DATE | DATE  |  The value to set under given key.  |
| Value | DATE_TIME | DATE_TIME  |  The value to set under given key.  |
| Value | INTEGER | INTEGER  |  The value to set under given key.  |
| Value | NULL | NULL  |  The value to set under given key.  |
| Value | NUMBER | NUMBER  |  The value to set under given key.  |
| Value | {} | OBJECT_BUILDER  |  The value to set under given key.  |
| Value | STRING | TEXT  |  The value to set under given key.  |
| Value | TIME | TIME  |  The value to set under given key.  |
| Append a list as a single item | BOOLEAN | SELECT  |  When set to true, and the value is a list, it will be added as a single value rather than concatenating the lists.  |




### Atomic Increment
The numeric value can be incremented atomically, and the action can be used concurrently from multiple executions.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a value to increment.  |
| Scope | STRING | SELECT  |  The namespace to obtain a value from.  |
| Value to add | INTEGER | INTEGER  |  The value that can be added to the existing numeric value, which may have a negative value.  |


### Output



Type: INTEGER

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Await Get Value
Wait for a value under a specified key, until it's available.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a value to wait for.  |
| Scope | STRING | SELECT  |  The namespace to obtain a value from.  |
| Type | INTEGER | SELECT  |  The value type.  |
| Default value | [] | ARRAY_BUILDER  |  The default value to return if no value exists under the given key.  |
| Default value | BOOLEAN | SELECT  |  The default value to return if no value exists under the given key.  |
| Default value | DATE | DATE  |  The default value to return if no value exists under the given key.  |
| Default value | DATE_TIME | DATE_TIME  |  The default value to return if no value exists under the given key.  |
| Default value | INTEGER | INTEGER  |  The default value to return if no value exists under the given key.  |
| Default value | NULL | NULL  |  The default value to return if no value exists under the given key.  |
| Default value | NUMBER | NUMBER  |  The default value to return if no value exists under the given key.  |
| Default value | {} | OBJECT_BUILDER  |  The default value to return if no value exists under the given key.  |
| Default value | STRING | TEXT  |  The default value to return if no value exists under the given key.  |
| Default value | TIME | TIME  |  The default value to return if no value exists under the given key.  |
| Timeout (1 to 300 sec) | INTEGER | INTEGER  |  If a value is not found within the specified time, the action returns a null value. Therefore, the maximum wait time should be set accordingly.  |




### Delete Value
Remove a value associated with a key in the specified scope.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a value to delete, stored earlier in the selected scope.  |
| Scope | STRING | SELECT  |  The namespace to delete a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |




### Delete Value from List
Delete a value from the given index in a list.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a list to delete value from, stored earlier in the selected scope.  |
| Scope | STRING | SELECT  |  The namespace to delete a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |
| Index | INTEGER | INTEGER  |  The specified index in the list will be removed, and if it doesn't exist, the list will remain unaltered.  |




### Get All Entries(Keys and Values)
Retrieve all the currently existing keys from storage, along with their values within the provided scope.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Scope | STRING | SELECT  |  The namespace to get keys from.  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Get Value
Retrieve a previously assigned value within the specified scope using its corresponding key.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a value to get, stored earlier in the selected scope.  |
| Scope | STRING | SELECT  |  The namespace to get a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |
| Type | INTEGER | SELECT  |  The value type.  |
| Default value | [] | ARRAY_BUILDER  |  The default value to return if no value exists under the given key.  |
| Default value | BOOLEAN | SELECT  |  The default value to return if no value exists under the given key.  |
| Default value | DATE | DATE  |  The default value to return if no value exists under the given key.  |
| Default value | DATE_TIME | DATE_TIME  |  The default value to return if no value exists under the given key.  |
| Default value | INTEGER | INTEGER  |  The default value to return if no value exists under the given key.  |
| Default value | NULL | NULL  |  The default value to return if no value exists under the given key.  |
| Default value | NUMBER | NUMBER  |  The default value to return if no value exists under the given key.  |
| Default value | {} | OBJECT_BUILDER  |  The default value to return if no value exists under the given key.  |
| Default value | STRING | TEXT  |  The default value to return if no value exists under the given key.  |
| Default value | TIME | TIME  |  The default value to return if no value exists under the given key.  |




### Set Value
Set a value under a key, in the specified scope.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a value. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.  |
| Scope | STRING | SELECT  |  The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |
| Type | INTEGER | SELECT  |  The value type.  |
| Value | [] | ARRAY_BUILDER  |  The value to set under the specified key.  |
| Value | BOOLEAN | SELECT  |  The value to set under the specified key.  |
| Value | DATE | DATE  |  The value to set under the specified key.  |
| Value | DATE_TIME | DATE_TIME  |  The value to set under the specified key.  |
| Value | INTEGER | INTEGER  |  The value to set under the specified key.  |
| Value | NULL | NULL  |  The value to set under the specified key.  |
| Value | NUMBER | NUMBER  |  The value to set under the specified key.  |
| Value | {} | OBJECT_BUILDER  |  The value to set under the specified key.  |
| Value | STRING | TEXT  |  The value to set under the specified key.  |
| Value | TIME | TIME  |  The value to set under the specified key.  |




### Set Value in List
Set value under a specified index in a list.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  The identifier of a list. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.  |
| Scope | STRING | SELECT  |  The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |
| Index | INTEGER | INTEGER  |  The index in a list to set a value under. The previous value will be overridden.  |
| Type | INTEGER | SELECT  |  The value type.  |
| Value | [] | ARRAY_BUILDER  |  The value to set under the specified list's key.  |
| Value | BOOLEAN | SELECT  |  The value to set under the specified list's key.  |
| Value | DATE | DATE  |  The value to set under the specified list's key.  |
| Value | DATE_TIME | DATE_TIME  |  The value to set under the specified list's key.  |
| Value | INTEGER | INTEGER  |  The value to set under the specified key.  |
| Value | NULL | NULL  |  The value to set under the specified key.  |
| Value | NUMBER | NUMBER  |  The value to set under the specified list's key.  |
| Value | {} | OBJECT_BUILDER  |  The value to set under the specified list's key.  |
| Value | STRING | TEXT  |  The value to set under the specified list's key.  |
| Value | TIME | TIME  |  The value to set under the specified list's key.  |




