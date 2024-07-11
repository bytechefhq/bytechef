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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File Type | STRING | SELECT  |
| File | FILE_ENTRY | FILE_ENTRY  |
| Is Array | BOOLEAN | SELECT  |
| Path | STRING | TEXT  |
| Page Size | INTEGER | INTEGER  |
| Page Number | INTEGER | INTEGER  |




### Write to file
Writes the data to a JSON file.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File Type | STRING | SELECT  |
| Type | INTEGER | SELECT  |
| Source | OBJECT | OBJECT_BUILDER  |
| Source | ARRAY | ARRAY_BUILDER  |
| Filename | STRING | TEXT  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





