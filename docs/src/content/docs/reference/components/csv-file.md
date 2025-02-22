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
Name: read

Reads data from a csv file.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | The object property which contains a reference to the csv file to read from. | true |
| delimiter | Delimiter | STRING | Character used to separate values within the line red from the CSV file. | null |
| enclosingCharacter | Enclosing Character | STRING | Character used to wrap/enclose values. It is usually applied to complex CSV files where values may include delimiter characters. | null |
| headerRow | Header Row | BOOLEAN <details> <summary> Options </summary> true, false </details> | The first row of the file contains the header names. | null |
| includeEmptyCells | Include Empty Cells | BOOLEAN <details> <summary> Options </summary> true, false </details> | When reading from file the empty cells will be filled with an empty string. | null |
| pageSize | Page Size | INTEGER | The amount of child elements to return in a page. | null |
| pageNumber | Page Number | INTEGER | The page number to get. | null |
| readAsString | Read as String | BOOLEAN <details> <summary> Options </summary> true, false </details> | In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way. | null |


#### JSON Example
```json
{
  "label" : "Read from File",
  "name" : "read",
  "parameters" : {
    "fileEntry" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    },
    "delimiter" : "",
    "enclosingCharacter" : "",
    "headerRow" : false,
    "includeEmptyCells" : false,
    "pageSize" : 1,
    "pageNumber" : 1,
    "readAsString" : false
  },
  "type" : "csvFile/v1/read"
}
```


### Write to CSV File
Name: write

Writes the data records into a CSV file. Record values are assembled into line and separated with arbitrary character, mostly comma. CSV may or may not define header line.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| rows | Rows | ARRAY <details> <summary> Items </summary> [{}] </details> | The array of rows to write to the file. | true |
| filename | Filename | STRING | Filename to set for binary data. By default, "file.csv" will be used. | null |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| extension | STRING |
| mimeType | STRING |
| name | STRING |
| url | STRING |




#### JSON Example
```json
{
  "label" : "Write to CSV File",
  "name" : "write",
  "parameters" : {
    "rows" : [ { } ],
    "filename" : ""
  },
  "type" : "csvFile/v1/write"
}
```




