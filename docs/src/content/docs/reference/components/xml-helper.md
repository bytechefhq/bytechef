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
Name: parse

Converts the XML string to object/array.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| source | Source | STRING | TEXT | The XML string to convert to the data. | true |


#### Output



Type: OBJECT





#### JSON Example
```json
{
  "label" : "Convert from XML String",
  "name" : "parse",
  "parameters" : {
    "source" : ""
  },
  "type" : "xmlHelper/v1/parse"
}
```


### Convert to XML String
Name: stringify

Writes the object/array to a XML string.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| type | Type | STRING <details> <summary> Options </summary> OBJECT, ARRAY </details> | SELECT | The value type. | null |
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The object to convert to XML string. | true |
| source | Source | ARRAY <details> <summary> Items </summary> [] </details> | ARRAY_BUILDER | The array to convert to XML string. | true |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "Convert to XML String",
  "name" : "stringify",
  "parameters" : {
    "type" : "",
    "source" : [ ]
  },
  "type" : "xmlHelper/v1/stringify"
}
```




