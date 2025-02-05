---
title: "ODS File"
description: "Reads and writes data from a ODS file."
---

Reads and writes data from a ODS file.


Categories: helpers


Type: odsFile/v1

<hr />




## Actions


### Read from File
Reads data from a ODS file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the ODS file to read from.  |  true  |
| sheetName | Sheet Name | STRING | TEXT  |  The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.  |  null  |
| headerRow | Header Row | BOOLEAN | SELECT  |  The first row of the file contains the header names.  |  null  |
| includeEmptyCells | Include Empty Cells | BOOLEAN | SELECT  |  When reading from file the empty cells will be filled with an empty string.  |  null  |
| pageSize | Page Size | INTEGER | INTEGER  |  The amount of child elements to return in a page.  |  null  |
| pageNumber | Page Number | INTEGER | INTEGER  |  The page number to get.  |  null  |
| readAsString | Read as String | BOOLEAN | SELECT  |  In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.  |  null  |




### Write to File
Writes the data to a ODS file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| sheetName | Sheet Name | STRING | TEXT  |  The name of the sheet to create in the spreadsheet.  |  null  |
| rows | Rows | [{}] | ARRAY_BUILDER  |  The array of rows to write to the file.  |  true  |
| filename | Filename | STRING | TEXT  |  Filename to set for binary data. By default, "file.ods" will be used.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |








