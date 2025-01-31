---
title: "Google Sheets"
description: "Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate on spreadsheets in real-time."
---
## Reference
<hr />

Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate on spreadsheets in real-time.


Categories: [productivity-and-collaboration]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers


### New Row
Triggers when a new row is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |
| Sheet | STRING | SELECT  |  The name of the sheet  |





<hr />



## Actions


### Clear Sheet
Clear a sheet of all values while preserving formats.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |
| Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |




### Create Column
Append a new column to the end of the sheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet Name | STRING | SELECT  |  The name of the sheet.  |
| Column Name | STRING | TEXT  |  Name of the new column.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |






### Create Sheet
Create a blank sheet with title. Optionally, provide headers.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet Name | STRING | TEXT  |  The name of the new sheet.  |
| Headers | [STRING] | ARRAY_BUILDER  |  The headers of the new sheet.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |






### Delete Column
Delete column on an existing sheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |
| Column Label | STRING | TEXT  |  The label of the column to be deleted.  |




### Delete Row
Delete row on an existing sheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |
| Row Number | INTEGER | INTEGER  |  The row number to delete.  |




### Delete Sheet
Delete a specified sheet from a spreadsheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet ID | INTEGER | SELECT  |  The ID of the sheet.  |




### Find Row by Number
Get a row in a Google Sheet by row number.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet Name | STRING | SELECT  |  The name of the sheet.  |
| Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |
| Row Number | INTEGER | INTEGER  |  The row number to get from the sheet.  |




### Insert Multiple Rows
Append rows to the end of the spreadsheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet Name | STRING | SELECT  |  The name of the sheet.  |
| Value Input Option | STRING | SELECT  |  How the input data should be interpreted.  |
| Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |
| DYNAMIC_PROPERTIES | null  |




### Insert Row
Append a row of values to an existing sheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet Name | STRING | SELECT  |  The name of the sheet.  |
| Value Input Option | STRING | SELECT  |  How the input data should be interpreted.  |
| Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |
| DYNAMIC_PROPERTIES | null  |




### Update Row
Overwrite values in an existing row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet ID | STRING | SELECT  |  The ID of the spreadsheet to apply the updates to.  |
| Sheet Name | STRING | SELECT  |  The name of the sheet.  |
| Row Number | INTEGER | INTEGER  |  The row number to update.  |
| Is the First Row Headers? | BOOLEAN | SELECT  |  If the first row is header.  |
| Update Whole Row | BOOLEAN | SELECT  |  Whether to update the whole row or just specific columns.  |
| DYNAMIC_PROPERTIES | null  |




<hr />

# Additional instructions
<hr />

![anl-c-google-sheet-md](https://static.scarf.sh/a.png?x-pxid=825c028e-5578-4a96-841e-0c91c0fa1134)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Sheets API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/61d6b773-ad2d-49c3-9c9c-d0b906cd5086?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
