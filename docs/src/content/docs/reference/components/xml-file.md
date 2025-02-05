---
title: "XML File"
description: "Reads and writes data from a XML file."
---

Reads and writes data from a XML file.


Categories: helpers


Type: xmlFile/v1

<hr />




## Actions


### Read from File
Reads data from a XML file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the XML file to read from.  |  true  |
| isArray | Is Array | BOOLEAN | SELECT  |  The object input is array?  |  null  |
| path | Path | STRING | TEXT  |  The path where the array is e.g 'data'. Leave blank to use the top level object.  |  null  |
| pageSize | Page Size | INTEGER | INTEGER  |  The amount of child elements to return in a page.  |  null  |
| pageNumber | Page Number | INTEGER | INTEGER  |  The page number to get.  |  null  |




### Write to File
Writes the data to a XML file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| type | Type | STRING | SELECT  |  The value type.  |  null  |
| source | Source | {} | OBJECT_BUILDER  |  The object to write to the file.  |  true  |
| source | Source | [] | ARRAY_BUILDER  |  The aray to write to the file.  |  true  |
| filename | Filename | STRING | TEXT  |  Filename to set for binary data. By default, "file.xml" will be used.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |








