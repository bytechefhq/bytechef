---
title: "Random Helper"
description: "The Random Helper allows you to generate random values."
---

The Random Helper allows you to generate random values.


Categories: helpers


Type: randomHelper/v1

<hr />




## Actions


### Random Integer
Name: randomInt

Generates a random integer value.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| startInclusive | Start Inclusive | INTEGER | The minimum possible generated value. | true |
| endInclusive | End Inclusive | INTEGER | The maximum possible generated value. | true |


#### Output



Type: INTEGER





#### JSON Example
```json
{
  "label" : "Random Integer",
  "name" : "randomInt",
  "parameters" : {
    "startInclusive" : 1,
    "endInclusive" : 1
  },
  "type" : "randomHelper/v1/randomInt"
}
```


### Random Float
Name: randomFloat

Generates a random float value.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| startInclusive | Start Inclusive | INTEGER | The minimum possible generated value. | true |
| endInclusive | End Inclusive | INTEGER | The maximum possible generated value. | true |


#### Output



Type: NUMBER





#### JSON Example
```json
{
  "label" : "Random Float",
  "name" : "randomFloat",
  "parameters" : {
    "startInclusive" : 1,
    "endInclusive" : 1
  },
  "type" : "randomHelper/v1/randomFloat"
}
```




