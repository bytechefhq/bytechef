---
title: "Object Helper"
description: "Object Helper allows you to do various operations on objects."
---
## Reference
<hr />

Object Helper allows you to do various operations on objects.


Categories: [HELPERS]


Version: 1

<hr />






## Actions


### Convert from JSON string
Converts the JSON string to object/array.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Source | STRING | TEXT  |  The JSON string to convert to the data.  |




### Convert to JSON string
Writes the object/array to a JSON string.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Type | INTEGER | SELECT  |  The value type.  |
| Source | {} | OBJECT_BUILDER  |  The data to convert to JSON string.  |
| Source | [] | ARRAY_BUILDER  |  The data to convert to JSON string.  |




