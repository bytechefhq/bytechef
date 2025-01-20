---
title: "JSON Helper"
description: "JSON helper component provides actions for parsing and stringifying JSON."
---
## Reference
<hr />

JSON helper component provides actions for parsing and stringifying JSON.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Convert from JSON String
Converts the JSON string to object/array.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Source | STRING | TEXT  |  The JSON string to convert to the data.  |




### Convert to JSON String
Writes the object/array to a JSON string.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Type | STRING | SELECT  |  The value type.  |
| Source | {} | OBJECT_BUILDER  |  The data to convert to JSON string.  |
| Source | [] | ARRAY_BUILDER  |  The data to convert to JSON string.  |




