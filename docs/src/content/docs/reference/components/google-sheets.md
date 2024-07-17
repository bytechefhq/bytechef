---
title: "Google Sheets"
description: "Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate on spreadsheets in real-time."
---
## Reference
<hr />

Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate on spreadsheets in real-time.


Categories: [PRODUCTIVITY_AND_COLLABORATION]


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


### OnRowAdded
Triggers when you add a row in google sheets. Refresh the page when you're done putting input.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Is the first row headers? | BOOLEAN | SELECT  |  If the first row is header  |
| Sheet | STRING | SELECT  |  The name of the sheet  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null






<hr />



## Actions


### Clear sheet
Clear a sheet of all values while preserving formats.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Include sheets from all drives | BOOLEAN | SELECT  |  Whether both My Drive and shared drive sheets should be included in results.  |
| Sheet | INTEGER | SELECT  |  The name of the sheet  |
| Is the first row headers? | BOOLEAN | SELECT  |  If the first row is header  |




### Delete row
Delete row on an existing sheet

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Include sheets from all drives | BOOLEAN | SELECT  |  Whether both My Drive and shared drive sheets should be included in results.  |
| Sheet | INTEGER | SELECT  |  The name of the sheet  |
| Row number | INTEGER | INTEGER  |  The row number to delete  |




### Find row by number
Get a row in a Google Sheet by row number

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Include sheets from all drives | BOOLEAN | SELECT  |  Whether both My Drive and shared drive sheets should be included in results.  |
| Sheet | STRING | SELECT  |  The name of the sheet  |
| Is the first row headers? | BOOLEAN | SELECT  |  If the first row is header  |
| Row number | INTEGER | INTEGER  |  The row number to get from the sheet.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Insert row
Append a row of values to an existing sheet

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Include sheets from all drives | BOOLEAN | SELECT  |  Whether both My Drive and shared drive sheets should be included in results.  |
| Sheet | STRING | SELECT  |  The name of the sheet  |
| Value input option | STRING | SELECT  |  How the input data should be interpreted.  |
| Is the first row headers? | BOOLEAN | SELECT  |  If the first row is header  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Update row
Overwrite values in an existing row

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Spreadsheet | STRING | SELECT  |  The spreadsheet to apply the updates to.  |
| Include sheets from all drives | BOOLEAN | SELECT  |  Whether both My Drive and shared drive sheets should be included in results.  |
| Sheet | STRING | SELECT  |  The name of the sheet  |
| Row number | INTEGER | INTEGER  |  The row number to update  |
| Is the first row headers? | BOOLEAN | SELECT  |  If the first row is header  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





<hr />

# Additional instructions
<hr />

![anl-c-google-sheet-md](https://static.scarf.sh/a.png?x-pxid=825c028e-5578-4a96-841e-0c91c0fa1134)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

[Guidejar](https://guidejar.com/guides/fec74020-26bb-43dd-814c-f8b907f6f45b) tutorial.

[Turning on Sheets API](https://guidejar.com/guides/61d6b773-ad2d-49c3-9c9c-d0b906cd5086)
