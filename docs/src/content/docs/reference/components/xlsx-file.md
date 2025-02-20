---
title: "XLSX File"
description: "Reads and writes data from a XLS/XLSX file."
---

Reads and writes data from a XLS/XLSX file.


Categories: helpers


Type: xlsxFile/v1

<hr />




## Actions


### Read from File
Name: read

Reads data from a XLS/XLSX file.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY | The object property which contains a reference to the XLS/XLSX file to read from. | true |
| sheetName | Sheet Name | STRING | TEXT | The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen. | null |
| headerRow | Header Row | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | The first row of the file contains the header names. | null |
| includeEmptyCells | Include Empty Cells | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | When reading from file the empty cells will be filled with an empty string. | null |
| pageSize | Page Size | INTEGER | INTEGER | The amount of child elements to return in a page. | null |
| pageNumber | Page Number | INTEGER | INTEGER | The page number to get. | null |
| readAsString | Read As String | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way. | null |


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
    "sheetName" : "",
    "headerRow" : false,
    "includeEmptyCells" : false,
    "pageSize" : 1,
    "pageNumber" : 1,
    "readAsString" : false
  },
  "type" : "xlsxFile/v1/read"
}
```


### Write to File
Name: write

Writes the data to a XLS/XLSX file.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| sheetName | Sheet Name | STRING | TEXT | The name of the sheet to create in the spreadsheet. | null |
| rows | Rows | ARRAY <details> <summary> Items </summary> [{}] </details> | ARRAY_BUILDER | The array of rows to write to the file. | true |
| filename | Filename | STRING | TEXT | Filename to set for binary data. By default, "file.xlsx" will be used. | true |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| extension | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |
| url | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Write to File",
  "name" : "write",
  "parameters" : {
    "sheetName" : "",
    "rows" : [ { } ],
    "filename" : ""
  },
  "type" : "xlsxFile/v1/write"
}
```




