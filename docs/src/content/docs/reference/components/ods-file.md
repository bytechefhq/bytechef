---
title: "ODS File"
description: "Reads and writes data from a ODS file."
---
## Reference
<hr />

Reads and writes data from a ODS file.

Categories: [HELPERS]

Version: 1

<hr />






## Actions


### Read from file
Reads data from a ODS file.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Sheet Name | STRING | TEXT  |
| Header Row | BOOLEAN | SELECT  |
| Include Empty Cells | BOOLEAN | SELECT  |
| Page Size | INTEGER | INTEGER  |
| Page Number | INTEGER | INTEGER  |
| Read As String | BOOLEAN | SELECT  |




### Write to file
Writes the data to a ODS file.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Sheet Name | STRING | TEXT  |
| Rows | ARRAY | ARRAY_BUILDER  |
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





