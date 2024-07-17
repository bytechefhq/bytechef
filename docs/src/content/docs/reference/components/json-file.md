---
title: "JSON File"
description: "Reads and writes data from a JSON file."
---
## Reference
<hr />

Reads and writes data from a JSON file.


Categories: [HELPERS]


Version: 1

<hr />






## Actions


### Read from file
Reads data from a JSON file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File Type | STRING | SELECT  |  The file type to choose.  |
| File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the JSON file to read from.  |
| Is Array | BOOLEAN | SELECT  |  The object input is array?  |
| Path | STRING | TEXT  |  The path where the array is e.g 'data'. Leave blank to use the top level object.  |
| Page Size | INTEGER | INTEGER  |  The amount of child elements to return in a page.  |
| Page Number | INTEGER | INTEGER  |  The page number to get.  |




### Write to file
Writes the data to a JSON file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File Type | STRING | SELECT  |  The file type to choose.  |
| Type | INTEGER | SELECT  |  The value type.  |
| Source | {} | OBJECT_BUILDER  |  The object to write to the file.  |
| Source | [] | ARRAY_BUILDER  |  The array to write to the file.  |
| Filename | STRING | TEXT  |  Filename to set for binary data. By default, "file.json" will be used.  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





