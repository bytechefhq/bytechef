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
Assign value to a variable that can be used in the following steps.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| value | Value | [] | ARRAY_BUILDER  |  Value of any type to set.  |  true  |
| value | Value | BOOLEAN | SELECT  |  Value of any type to set.  |  true  |
| value | Value | DATE | DATE  |  Value of any type to set.  |  true  |
| value | Value | DATE_TIME | DATE_TIME  |  Value of any type to set.  |  true  |
| value | Value | INTEGER | INTEGER  |  Value of any type to set.  |  true  |
| value | Value | NULL | NULL  |  Value of any type to set.  |  true  |
| value | Value | NUMBER | NUMBER  |  Value of any type to set.  |  true  |
| value | Value | {} | OBJECT_BUILDER  |  Value of any type to set.  |  true  |
| value | Value | STRING | TEXT  |  Value of any type to set.  |  true  |
| value | Value | TIME | TIME  |  Value of any type to set.  |  true  |






