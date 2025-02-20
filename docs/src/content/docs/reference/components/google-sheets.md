---
title: "Google Sheets"
description: "Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate on spreadsheets in real-time."
---

Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate on spreadsheets in real-time.


Categories: productivity-and-collaboration


Type: googleSheets/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Clear Sheet
Name: clearSheet

Clear a sheet of all values while preserving formats.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetId | Sheet ID | INTEGER <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The ID of the sheet. | true |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |


#### JSON Example
```json
{
  "label" : "Clear Sheet",
  "name" : "clearSheet",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetId" : 1,
    "isTheFirstRowHeader" : false
  },
  "type" : "googleSheets/v1/clearSheet"
}
```


### Create Column
Name: createColumn

Append a new column to the end of the sheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetName | Sheet Name | STRING <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The name of the sheet. | true |
| columnName | Column Name | STRING | TEXT | Name of the new column. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| spreadsheetId | STRING | TEXT |
| sheetName | STRING | TEXT |
| headers | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Column",
  "name" : "createColumn",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetName" : "",
    "columnName" : ""
  },
  "type" : "googleSheets/v1/createColumn"
}
```


### Create Sheet
Name: createSheet

Create a blank sheet with title. Optionally, provide headers.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetName | Sheet Name | STRING | TEXT | The name of the new sheet. | true |
| headers | Headers | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | The headers of the new sheet. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| spreadsheetId | STRING | TEXT |
| sheetName | STRING | TEXT |
| headers | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Sheet",
  "name" : "createSheet",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetName" : "",
    "headers" : [ "" ]
  },
  "type" : "googleSheets/v1/createSheet"
}
```


### Delete Column
Name: deleteColumn

Delete column on an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetId | Sheet ID | INTEGER <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The ID of the sheet. | true |
| label | Column Label | STRING | TEXT | The label of the column to be deleted. | true |


#### JSON Example
```json
{
  "label" : "Delete Column",
  "name" : "deleteColumn",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetId" : 1,
    "label" : ""
  },
  "type" : "googleSheets/v1/deleteColumn"
}
```


### Create Spreadsheet
Name: createSpreadsheet

Create a new spreadsheet in a specified folder.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| title | Title | STRING | TEXT | Title of the new spreadsheet to be created. | true |
| folderId | Folder ID | STRING | SELECT | ID of the folder where the new spreadsheet will be stored. If no folder is selected, the folder will be created in the root folder. | false |


#### JSON Example
```json
{
  "label" : "Create Spreadsheet",
  "name" : "createSpreadsheet",
  "parameters" : {
    "title" : "",
    "folderId" : ""
  },
  "type" : "googleSheets/v1/createSpreadsheet"
}
```


### Delete Row
Name: deleteRow

Delete row on an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetId | Sheet ID | INTEGER <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The ID of the sheet. | true |
| rowNumber | Row Number | INTEGER | INTEGER | The row number to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Row",
  "name" : "deleteRow",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetId" : 1,
    "rowNumber" : 1
  },
  "type" : "googleSheets/v1/deleteRow"
}
```


### Delete Sheet
Name: deleteSheet

Delete a specified sheet from a spreadsheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetId | Sheet ID | INTEGER <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The ID of the sheet. | true |


#### JSON Example
```json
{
  "label" : "Delete Sheet",
  "name" : "deleteSheet",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetId" : 1
  },
  "type" : "googleSheets/v1/deleteSheet"
}
```


### Find Row by Number
Name: findRowByNum

Get a row in a Google Sheet by row number.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetName | Sheet Name | STRING <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The name of the sheet. | true |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| rowNumber | Row Number | INTEGER | INTEGER | The row number to get from the sheet. | true |


#### JSON Example
```json
{
  "label" : "Find Row by Number",
  "name" : "findRowByNum",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetName" : "",
    "isTheFirstRowHeader" : false,
    "rowNumber" : 1
  },
  "type" : "googleSheets/v1/findRowByNum"
}
```


### Insert Multiple Rows
Name: insertMultipleRows

Append rows to the end of the spreadsheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetName | Sheet Name | STRING <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The name of the sheet. | true |
| valueInputOption | Value Input Option | STRING <details> <summary> Options </summary> RAW, USER_ENTERED </details> | SELECT | How the input data should be interpreted. | true |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| rows | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> spreadsheetId, sheetName, isTheFirstRowHeader </details> | null |  | true |


#### JSON Example
```json
{
  "label" : "Insert Multiple Rows",
  "name" : "insertMultipleRows",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetName" : "",
    "valueInputOption" : "",
    "isTheFirstRowHeader" : false,
    "rows" : { }
  },
  "type" : "googleSheets/v1/insertMultipleRows"
}
```


### Insert Row
Name: insertRow

Append a row of values to an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetName | Sheet Name | STRING <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The name of the sheet. | true |
| valueInputOption | Value Input Option | STRING <details> <summary> Options </summary> RAW, USER_ENTERED </details> | SELECT | How the input data should be interpreted. | true |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| row | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> spreadsheetId, sheetName, isTheFirstRowHeader </details> | null |  | true |


#### JSON Example
```json
{
  "label" : "Insert Row",
  "name" : "insertRow",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetName" : "",
    "valueInputOption" : "",
    "isTheFirstRowHeader" : false,
    "row" : { }
  },
  "type" : "googleSheets/v1/insertRow"
}
```


### List Sheets
Name: listSheets

Get all sheets from the spreadsheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| spreadsheetId | STRING | TEXT |
| sheetId | INTEGER | INTEGER |
| sheetName | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "List Sheets",
  "name" : "listSheets",
  "parameters" : {
    "spreadsheetId" : ""
  },
  "type" : "googleSheets/v1/listSheets"
}
```


### Update Row
Name: updateRow

Overwrite values in an existing row.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT | The ID of the spreadsheet to apply the updates to. | true |
| sheetName | Sheet Name | STRING <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The name of the sheet. | true |
| rowNumber | Row Number | INTEGER | INTEGER | The row number to update. | true |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| updateWholeRow | Update Whole Row | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Whether to update the whole row or just specific columns. | true |
| row | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> spreadsheetId, sheetName, isTheFirstRowHeader, updateWholeRow </details> | null |  | true |


#### JSON Example
```json
{
  "label" : "Update Row",
  "name" : "updateRow",
  "parameters" : {
    "spreadsheetId" : "",
    "sheetName" : "",
    "rowNumber" : 1,
    "isTheFirstRowHeader" : false,
    "updateWholeRow" : false,
    "row" : { }
  },
  "type" : "googleSheets/v1/updateRow"
}
```




## Triggers


### New Row
Name: newRow

Triggers when a new row is added.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| spreadsheetId | Spreadsheet | STRING | SELECT | The spreadsheet to apply the updates to. | true |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| sheetName | Sheet | STRING <details> <summary> Depends On </summary> spreadsheetId </details> | SELECT | The name of the sheet | true |


#### JSON Example
```json
{
  "label" : "New Row",
  "name" : "newRow",
  "parameters" : {
    "spreadsheetId" : "",
    "isTheFirstRowHeader" : false,
    "sheetName" : ""
  },
  "type" : "googleSheets/v1/newRow"
}
```


<hr />

<hr />

# Additional instructions
<hr />

![anl-c-google-sheet-md](https://static.scarf.sh/a.png?x-pxid=825c028e-5578-4a96-841e-0c91c0fa1134)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Sheets API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/61d6b773-ad2d-49c3-9c9c-d0b906cd5086?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
