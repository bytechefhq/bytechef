---
title: "CSV File"
description: "Reads and writes data from a csv file."
---

Reads and writes data from a csv file.


Categories: helpers


Type: csvFile/v1

<hr />




## Actions


### Read from File
Reads data from a csv file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the csv file to read from.  |  true  |
| delimiter | Delimiter | STRING | TEXT  |  Character used to separate values within the line red from the CSV file.  |  null  |
| enclosingCharacter | Enclosing Character | STRING | TEXT  |      Character used to wrap/enclose values. It is usually applied to complex CSV files where
    values may include delimiter characters.
  |  null  |
| headerRow | Header Row | BOOLEAN | SELECT  |  The first row of the file contains the header names.  |  null  |
| includeEmptyCells | Include Empty Cells | BOOLEAN | SELECT  |  When reading from file the empty cells will be filled with an empty string.  |  null  |
| pageSize | Page Size | INTEGER | INTEGER  |  The amount of child elements to return in a page.  |  null  |
| pageNumber | Page Number | INTEGER | INTEGER  |  The page number to get.  |  null  |
| readAsString | Read as String | BOOLEAN | SELECT  |  In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.  |  null  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {} | OBJECT_BUILDER  |






### Write to CSV File
Writes the data records into a CSV file. Record values are assembled into line and separated with arbitrary character, mostly comma. CSV may or may not define header line.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| rows | Rows | [{}] | ARRAY_BUILDER  |  The array of rows to write to the file.  |  true  |
| filename | Filename | STRING | TEXT  |  Filename to set for binary data. By default, "file.csv" will be used.  |  null  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |








