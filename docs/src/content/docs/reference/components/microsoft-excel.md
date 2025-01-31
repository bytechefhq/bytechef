---
title: "Microsoft Excel"
description: "Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in tabular form."
---
## Reference
<hr />

Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in tabular form.


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
| Tenant Id | STRING | TEXT  |  |





<hr />





## Actions


### Append Row
Append a row of values to an existing worksheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workbook ID | STRING | SELECT  |  |
| Worksheet | STRING | SELECT  |  |
| Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |
| DYNAMIC_PROPERTIES | null  |




### Clear Worksheet
Clear a worksheet of all values.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workbook ID | STRING | SELECT  |  |
| Worksheet | STRING | SELECT  |  |
| Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |




### Delete Row
Delete row on an existing sheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workbook ID | STRING | SELECT  |  |
| Worksheet | STRING | SELECT  |  |
| Row Number | INTEGER | INTEGER  |  The row number to delete.  |




### Find Row by Number
Get row values from the worksheet by the row number.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workbook ID | STRING | SELECT  |  |
| Worksheet | STRING | SELECT  |  |
| Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |
| Row Number | INTEGER | INTEGER  |  The row number to get the values from.  |




### Update Row
Update a row in a worksheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workbook ID | STRING | SELECT  |  |
| Worksheet | STRING | SELECT  |  |
| Row Number | INTEGER | INTEGER  |  The row number to update.  |
| Is the First Row Header? | BOOLEAN | SELECT  |  If the first row is header.  |
| DYNAMIC_PROPERTIES | null  |




