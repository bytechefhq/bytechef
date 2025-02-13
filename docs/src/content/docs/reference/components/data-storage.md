---
title: "Data Storage"
description: "Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value."
---

Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value.


Categories: helpers


Type: dataStorage/v1

<hr />




## Actions


### Append Value to List
Name: appendValueToList

Append value to the end of a list. If the list does not exist, it will be created.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| scope | Scope | STRING | SELECT  |  The namespace for appending a value.  |  true  |
| key | Key | STRING | TEXT  |  The identifier of a list must be unique within the chosen scope, or a new value will overwrite the existing one.  |  true  |
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| value | Value | [] | ARRAY_BUILDER  |  The value to set under given key.  |  true  |
| value | Value | BOOLEAN | SELECT  |  The value to set under given key.  |  true  |
| value | Value | DATE | DATE  |  The value to set under given key.  |  true  |
| value | Value | DATE_TIME | DATE_TIME  |  The value to set under given key.  |  true  |
| value | Value | INTEGER | INTEGER  |  The value to set under given key.  |  true  |
| value | Value | NULL | NULL  |  The value to set under given key.  |  true  |
| value | Value | NUMBER | NUMBER  |  The value to set under given key.  |  true  |
| value | Value | {} | OBJECT_BUILDER  |  The value to set under given key.  |  true  |
| value | Value | STRING | TEXT  |  The value to set under given key.  |  true  |
| value | Value | TIME | TIME  |  The value to set under given key.  |  true  |
| appendListAsSingleItem | Append a List as a Single Item | BOOLEAN | SELECT  |  When set to true, and the value is a list, it will be added as a single value rather than concatenating the lists.  |  null  |




### Atomic Increment
Name: atomicIncrement

The numeric value can be incremented atomically, and the action can be used concurrently from multiple executions.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a value to increment.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to obtain a value from.  |  true  |
| valueToAdd | Value to Add | INTEGER | INTEGER  |  The value that can be added to the existing numeric value, which may have a negative value.  |  null  |


#### Output



Type: INTEGER







### Await Get Value
Name: awaitGetValue

Wait for a value under a specified key, until it's available.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a value to wait for.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to obtain a value from.  |  true  |
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| defaultValue | Default Value | [] | ARRAY_BUILDER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | BOOLEAN | SELECT  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | DATE | DATE  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | DATE_TIME | DATE_TIME  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | INTEGER | INTEGER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | NULL | NULL  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | NUMBER | NUMBER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | {} | OBJECT_BUILDER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | STRING | TEXT  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | TIME | TIME  |  The default value to return if no value exists under the given key.  |  true  |
| timeout | Timeout | INTEGER | INTEGER  |  If a value is not found within the specified time, the action returns a null value. Therefore, the maximum wait time should be set accordingly.  |  true  |




### Delete Value
Name: deleteValue

Remove a value associated with a key in the specified scope.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a value to delete, stored earlier in the selected scope.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to delete a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |  true  |




### Delete Value from List
Name: deleteValueFromlist

Delete a value from the given index in a list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a list to delete value from, stored earlier in the selected scope.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to delete a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |  true  |
| index | Index | INTEGER | INTEGER  |  The specified index in the list will be removed, and if it doesn't exist, the list will remain unaltered.  |  true  |




### Get All Entries(Keys and Values)
Name: getAllEntries

Retrieve all the currently existing keys from storage, along with their values within the provided scope.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| scope | Scope | STRING | SELECT  |  The namespace to get keys from.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {} | OBJECT_BUILDER  |






### Get Value
Name: getValue

Retrieve a previously assigned value within the specified scope using its corresponding key.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a value to get, stored earlier in the selected scope.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to get a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |  true  |
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| defaultValue | Default Value | [] | ARRAY_BUILDER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | BOOLEAN | SELECT  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | DATE | DATE  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | DATE_TIME | DATE_TIME  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | INTEGER | INTEGER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | NULL | NULL  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | NUMBER | NUMBER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | {} | OBJECT_BUILDER  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | STRING | TEXT  |  The default value to return if no value exists under the given key.  |  true  |
| defaultValue | Default Value | TIME | TIME  |  The default value to return if no value exists under the given key.  |  true  |




### Set Value
Name: setValue

Set a value under a key, in the specified scope.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a value. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |  true  |
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| value | Value | [] | ARRAY_BUILDER  |  The value to set under the specified key.  |  true  |
| value | Value | BOOLEAN | SELECT  |  The value to set under the specified key.  |  true  |
| value | Value | DATE | DATE  |  The value to set under the specified key.  |  true  |
| value | Value | DATE_TIME | DATE_TIME  |  The value to set under the specified key.  |  true  |
| value | Value | INTEGER | INTEGER  |  The value to set under the specified key.  |  true  |
| value | Value | NULL | NULL  |  The value to set under the specified key.  |  true  |
| value | Value | NUMBER | NUMBER  |  The value to set under the specified key.  |  true  |
| value | Value | {} | OBJECT_BUILDER  |  The value to set under the specified key.  |  true  |
| value | Value | STRING | TEXT  |  The value to set under the specified key.  |  true  |
| value | Value | TIME | TIME  |  The value to set under the specified key.  |  true  |




### Set Value in List
Name: setValueInList

Set value under a specified index in a list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  The identifier of a list. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.  |  true  |
| scope | Scope | STRING | SELECT  |  The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.  |  true  |
| index | Index | INTEGER | INTEGER  |  The index in a list to set a value under. The previous value will be overridden.  |  true  |
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| value | Value | [] | ARRAY_BUILDER  |  The value to set under the specified list's key.  |  true  |
| value | Value | BOOLEAN | SELECT  |  The value to set under the specified list's key.  |  true  |
| value | Value | DATE | DATE  |  The value to set under the specified list's key.  |  true  |
| value | Value | DATE_TIME | DATE_TIME  |  The value to set under the specified list's key.  |  true  |
| value | Value | INTEGER | INTEGER  |  The value to set under the specified key.  |  true  |
| value | Value | NULL | NULL  |  The value to set under the specified key.  |  true  |
| value | Value | NUMBER | NUMBER  |  The value to set under the specified list's key.  |  true  |
| value | Value | {} | OBJECT_BUILDER  |  The value to set under the specified list's key.  |  true  |
| value | Value | STRING | TEXT  |  The value to set under the specified list's key.  |  true  |
| value | Value | TIME | TIME  |  The value to set under the specified list's key.  |  true  |






