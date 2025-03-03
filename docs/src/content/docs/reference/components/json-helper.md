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
Name: parse

Converts the JSON string to object/array.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| source | Source | STRING | The JSON string to convert to the data. | true |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Convert from JSON String",
  "name" : "parse",
  "parameters" : {
    "source" : ""
  },
  "type" : "jsonHelper/v1/parse"
}
```


### Convert to JSON String
Name: stringify

Writes the object/array to a JSON string.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| type | Type | STRING <details> <summary> Options </summary> OBJECT, ARRAY </details> | The value type. | null |
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | The data to convert to JSON string. | true |
| source | Source | ARRAY <details> <summary> Items </summary> [] </details> | The data to convert to JSON string. | true |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Convert to JSON String",
  "name" : "stringify",
  "parameters" : {
    "type" : "",
    "source" : [ ]
  },
  "type" : "jsonHelper/v1/stringify"
}
```




