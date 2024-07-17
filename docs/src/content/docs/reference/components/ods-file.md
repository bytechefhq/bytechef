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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the ODS file to read from.  |
| Sheet Name | STRING | TEXT  |  The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.  |
| Header Row | BOOLEAN | SELECT  |  The first row of the file contains the header names.  |
| Include Empty Cells | BOOLEAN | SELECT  |  When reading from file the empty cells will be filled with an empty string.  |
| Page Size | INTEGER | INTEGER  |  The amount of child elements to return in a page.  |
| Page Number | INTEGER | INTEGER  |  The page number to get.  |
| Read As String | BOOLEAN | SELECT  |  In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.  |




### Write to file
Writes the data to a ODS file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Sheet Name | STRING | TEXT  |  The name of the sheet to create in the spreadsheet.  |
| Rows | [{}] | ARRAY_BUILDER  |  The array of objects to write to the file.  |
| Filename | STRING | TEXT  |  Filename to set for binary data. By default, "file.ods" will be used.  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





