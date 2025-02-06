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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Clear Sheet
Clear a sheet of all values while preserving formats.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetId | Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |  true  |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |




### Create Column
Append a new column to the end of the sheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetName | Sheet Name | STRING | SELECT  |  The name of the sheet.  |  true  |
| columnName | Column Name | STRING | TEXT  |  Name of the new column.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| spreadsheetId | STRING | TEXT  |
| sheetName | STRING | TEXT  |
| headers | [STRING] | ARRAY_BUILDER  |






### Create Sheet
Create a blank sheet with title. Optionally, provide headers.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetName | Sheet Name | STRING | TEXT  |  The name of the new sheet.  |  true  |
| headers | Headers | [STRING] | ARRAY_BUILDER  |  The headers of the new sheet.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| spreadsheetId | STRING | TEXT  |
| sheetName | STRING | TEXT  |
| headers | [STRING] | ARRAY_BUILDER  |






### Delete Column
Delete column on an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetId | Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |  true  |
| label | Column Label | STRING | TEXT  |  The label of the column to be deleted.  |  true  |




### Create Spreadsheet
Create a new spreadsheet in a specified folder.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Title | STRING | TEXT  |  Title of the new spreadsheet to be created.  |  true  |
| folderId | Folder ID | STRING | SELECT  |  ID of the folder where the new spreadsheet will be stored. If no folder is selected, the folder will be created in the root folder.  |  false  |




### Delete Row
Delete row on an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetId | Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |  true  |
| rowNumber | Row Number | INTEGER | INTEGER  |  The row number to delete.  |  true  |




### Delete Sheet
Delete a specified sheet from a spreadsheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetId | Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |  true  |




### Find Row by Number
Get a row in a Google Sheet by row number.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetName | Sheet Name | STRING | SELECT  |  The name of the sheet.  |  true  |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| rowNumber | Row Number | INTEGER | INTEGER  |  The row number to get from the sheet.  |  true  |




### Insert Multiple Rows
Append rows to the end of the spreadsheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetName | Sheet Name | STRING | SELECT  |  The name of the sheet.  |  true  |
| valueInputOption | Value Input Option | STRING | SELECT  |  How the input data should be interpreted.  |  true  |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| rows | DYNAMIC_PROPERTIES | null  |




### Insert Row
Append a row of values to an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetName | Sheet Name | STRING | SELECT  |  The name of the sheet.  |  true  |
| valueInputOption | Value Input Option | STRING | SELECT  |  How the input data should be interpreted.  |  true  |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| row | DYNAMIC_PROPERTIES | null  |




### List Sheets
Get all sheets from the spreadsheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| spreadsheetId | STRING | TEXT  |
| sheetId | INTEGER | INTEGER  |
| sheetName | STRING | TEXT  |






### Update Row
Overwrite values in an existing row.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |  true  |
| sheetName | Sheet Name | STRING | SELECT  |  The name of the sheet.  |  true  |
| rowNumber | Row Number | INTEGER | INTEGER  |  The row number to update.  |  true  |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| updateWholeRow | Update Whole Row | BOOLEAN | SELECT  |  Whether to update the whole row or just specific columns.  |  true  |
| row | DYNAMIC_PROPERTIES | null  |






## Triggers


### New Row
Triggers when a new row is added.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spreadsheetId | Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |  true  |
| isTheFirstRowHeader | Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| sheetName | Sheet | STRING | SELECT  |  The name of the sheet  |  true  |





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
