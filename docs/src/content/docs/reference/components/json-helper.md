---
title: "JSON Helper"
description: "JSON helper component provides actions for parsing and stringifying JSON."
---

JSON helper component provides actions for parsing and stringifying JSON.


Categories: helpers


Type: jsonHelper/v1

<hr />




## Actions


### Convert from JSON String
Converts the JSON string to object/array.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| source | Source | STRING | TEXT  |  The JSON string to convert to the data.  |  true  |




### Convert to JSON String
Writes the object/array to a JSON string.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| source | Source | {} | OBJECT_BUILDER  |  The data to convert to JSON string.  |  true  |
| source | Source | [] | ARRAY_BUILDER  |  The data to convert to JSON string.  |  true  |






