---
title: "XML Helper"
description: "Converts between XML string and object/array."
---

Converts between XML string and object/array.


Categories: helpers


Type: xmlHelper/v1

<hr />




## Actions


### Convert from XML String
Converts the XML string to object/array.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| source | Source | STRING | TEXT  |  The XML string to convert to the data.  |  true  |


#### Output



Type: OBJECT







### Convert to XML String
Writes the object/array to a XML string.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| source | Source | {} | OBJECT_BUILDER  |  The object to convert to XML string.  |  true  |
| source | Source | [] | ARRAY_BUILDER  |  The array to convert to XML string.  |  true  |


#### Output



Type: STRING









