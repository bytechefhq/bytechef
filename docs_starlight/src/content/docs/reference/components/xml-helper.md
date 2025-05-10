---
title: "XML Helper"
description: "Converts between XML string and object/array."
---

Converts between XML string and object/array.


Categories: Helpers


Type: xmlHelper/v1

<hr />




## Actions


### Convert from XML String
Name: parse

Converts the XML string to object/array.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| source | Source | STRING | The XML string to convert to the data. | true |

#### Example JSON Structure
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

#### Output



Type: OBJECT








### Convert to XML String
Name: stringify

Writes the object/array to a XML string.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| type | Type | STRING <details> <summary> Options </summary> OBJECT, ARRAY </details> | The value type. | null |
| source | Source | OBJECT <details> <summary> Properties </summary> {} </details> | The object to convert to XML string. | true |
| source | Source | ARRAY <details> <summary> Items </summary> [] </details> | The array to convert to XML string. | true |

#### Example JSON Structure
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

#### Output



Type: STRING










