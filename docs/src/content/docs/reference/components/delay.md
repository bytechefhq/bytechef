---
title: "Delay"
description: "Sets a value which can then be referenced in other tasks."
---

Sets a value which can then be referenced in other tasks.


Categories: helpers


Type: delay/v1

<hr />




## Actions


### Sleep
Name: sleep

Delay action execution.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| millis | Millis | INTEGER | INTEGER | Time in milliseconds. | true |


#### JSON Example
```json
{
  "label" : "Sleep",
  "name" : "sleep",
  "parameters" : {
    "millis" : 1
  },
  "type" : "delay/v1/sleep"
}
```




