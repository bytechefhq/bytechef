---
title: "Microsoft Excel"
description: "Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in tabular form."
---

Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in tabular form.


Categories: productivity-and-collaboration


Type: microsoftExcel/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| tenantId | Tenant Id | STRING | TEXT  |  | true  |





<hr />



## Actions


### Append Row
Append a row of values to an existing worksheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workbookId | Workbook ID | STRING | SELECT  |  | true  |
| worksheetName | Worksheet | STRING | SELECT  |  | true  |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| row | DYNAMIC_PROPERTIES | null  |




### Clear Worksheet
Clear a worksheet of all values.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workbookId | Workbook ID | STRING | SELECT  |  | true  |
| worksheetName | Worksheet | STRING | SELECT  |  | true  |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |




### Delete Row
Delete row on an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workbookId | Workbook ID | STRING | SELECT  |  | true  |
| worksheetName | Worksheet | STRING | SELECT  |  | true  |
| rowNumber | Row Number | INTEGER | INTEGER  |  The row number to delete.  |  true  |




### Find Row by Number
Get row values from the worksheet by the row number.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workbookId | Workbook ID | STRING | SELECT  |  | true  |
| worksheetName | Worksheet | STRING | SELECT  |  | true  |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| rowNumber | Row Number | INTEGER | INTEGER  |  The row number to get the values from.  |  true  |




### Update Row
Update a row in a worksheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workbookId | Workbook ID | STRING | SELECT  |  | true  |
| worksheetName | Worksheet | STRING | SELECT  |  | true  |
| rowNumber | Row Number | INTEGER | INTEGER  |  The row number to update.  |  true  |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |  true  |
| row | DYNAMIC_PROPERTIES | null  |






