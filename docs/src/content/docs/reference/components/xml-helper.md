---
title: "XML Helper"
description: "Converts between XML string and object/array."
---
## Reference
<hr />

Converts between XML string and object/array.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Convert from XML String
Converts the XML string to object/array.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Source | STRING | TEXT  |  The XML string to convert to the data.  |


### Output



Type: OBJECT







### Convert to XML String
Writes the object/array to a XML string.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Type | INTEGER | SELECT  |  The value type.  |
| Source | {} | OBJECT_BUILDER  |  The object to convert to XML string.  |
| Source | [] | ARRAY_BUILDER  |  The array to convert to XML string.  |


### Output



Type: STRING







