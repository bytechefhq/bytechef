---
title: "Var"
description: "Sets a value which can then be referenced in other tasks."
---

Sets a value which can then be referenced in other tasks.


Categories: helpers


Type: var/v1

<hr />




## Actions


### Set Value
Name: set

Assign value to a variable that can be used in the following steps.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| type | Type | STRING <details> <summary> Options </summary> ARRAY, BOOLEAN, DATE, DATE_TIME, INTEGER, NUMBER, OBJECT, STRING, TIME </details> | SELECT | The value type. | null |
| value | Value | ARRAY <details> <summary> Items </summary> [] </details> | ARRAY_BUILDER | Value of any type to set. | true |
| value | Value | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Value of any type to set. | true |
| value | Value | DATE | DATE | Value of any type to set. | true |
| value | Value | DATE_TIME | DATE_TIME | Value of any type to set. | true |
| value | Value | INTEGER | INTEGER | Value of any type to set. | true |
| value | Value | NUMBER | NUMBER | Value of any type to set. | true |
| value | Value | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Value of any type to set. | true |
| value | Value | STRING | TEXT | Value of any type to set. | true |
| value | Value | TIME | TIME | Value of any type to set. | true |


#### JSON Example
```json
{
  "label" : "Set Value",
  "name" : "set",
  "parameters" : {
    "type" : "",
    "value" : "00:00:00"
  },
  "type" : "var/v1/set"
}
```




