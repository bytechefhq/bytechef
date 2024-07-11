---
title: "Microsoft Excel"
description: "Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in tabular form."
---
## Reference
<hr />

Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in tabular form.

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
| Tenant Id | STRING | TEXT  |





<hr />





## Actions


### Append row
Append a row of values to an existing worksheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Workbook | STRING | SELECT  |
| Worksheet | STRING | SELECT  |
| Is the first row header? | BOOLEAN | SELECT  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Clear worksheet
Clear a worksheet of all values.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Workbook | STRING | SELECT  |
| Worksheet | STRING | SELECT  |
| Is the first row header? | BOOLEAN | SELECT  |




### Delete row
Delete row on an existing sheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Workbook | STRING | SELECT  |
| Worksheet | STRING | SELECT  |
| Row number | INTEGER | INTEGER  |




### Find row by number
Get row values from the worksheet by the row number

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Workbook | STRING | SELECT  |
| Worksheet | STRING | SELECT  |
| Is the first row header? | BOOLEAN | SELECT  |
| Row number | INTEGER | INTEGER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Update row
Update a row in a worksheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Workbook | STRING | SELECT  |
| Worksheet | STRING | SELECT  |
| Row number | INTEGER | INTEGER  |
| Is the first row header? | BOOLEAN | SELECT  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





