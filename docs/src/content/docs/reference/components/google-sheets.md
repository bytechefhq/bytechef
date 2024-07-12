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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />



## Triggers


### OnRowAdded
Triggers when you add a row in google sheets. Refresh the page when you're done putting input.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Spreadsheet | STRING | SELECT  |
| Is the first row headers? | BOOLEAN | SELECT  |
| Sheet | STRING | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Spreadsheet | STRING | SELECT  |
| Include sheets from all drives | BOOLEAN | SELECT  |
| Sheet | INTEGER | SELECT  |
| Is the first row headers? | BOOLEAN | SELECT  |




### Delete row
Delete row on an existing sheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Spreadsheet | STRING | SELECT  |
| Include sheets from all drives | BOOLEAN | SELECT  |
| Sheet | INTEGER | SELECT  |
| Row number | INTEGER | INTEGER  |




### Find row by number
Get a row in a Google Sheet by row number

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Spreadsheet | STRING | SELECT  |
| Include sheets from all drives | BOOLEAN | SELECT  |
| Sheet | STRING | SELECT  |
| Is the first row headers? | BOOLEAN | SELECT  |
| Row number | INTEGER | INTEGER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Insert row
Append a row of values to an existing sheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Spreadsheet | STRING | SELECT  |
| Include sheets from all drives | BOOLEAN | SELECT  |
| Sheet | STRING | SELECT  |
| Value input option | STRING | SELECT  |
| Is the first row headers? | BOOLEAN | SELECT  |
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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Spreadsheet | STRING | SELECT  |
| Include sheets from all drives | BOOLEAN | SELECT  |
| Sheet | STRING | SELECT  |
| Row number | INTEGER | INTEGER  |
| Is the first row headers? | BOOLEAN | SELECT  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





