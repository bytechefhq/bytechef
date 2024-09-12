---
title: "CSV File"
description: "Reads and writes data from a csv file."
---
## Reference
<hr />

Reads and writes data from a csv file.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Read from file
Reads data from a csv file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the csv file to read from.  |
| Delimiter | STRING | TEXT  |  Character used to separate values within the line red from the CSV file.  |
| Enclosing character | STRING | TEXT  |      Character used to wrap/enclose values. It is usually applied to complex CSV files where
    values may include delimiter characters.
  |
| Header Row | BOOLEAN | SELECT  |  The first row of the file contains the header names.  |
| Include Empty Cells | BOOLEAN | SELECT  |  When reading from file the empty cells will be filled with an empty string.  |
| Page Size | INTEGER | INTEGER  |  The amount of child elements to return in a page.  |
| Page Number | INTEGER | INTEGER  |  The page number to get.  |
| Read As String | BOOLEAN | SELECT  |  In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {} | OBJECT_BUILDER  |






### Write to file
Writes the data to a csv file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Rows | [{}] | ARRAY_BUILDER  |  The array of objects to write to the file.  |
| Filename | STRING | TEXT  |  Filename to set for binary data. By default, "file.csv" will be used.  |


### Output



Type: FILE_ENTRY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






