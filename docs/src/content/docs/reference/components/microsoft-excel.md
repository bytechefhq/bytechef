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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| tenantId | Tenant Id | STRING | TEXT |  | true |





<hr />



## Actions


### Append Row
Name: appendRow

Append a row of values to an existing worksheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workbookId | Workbook ID | STRING | SELECT |  | true |
| worksheetName | Worksheet | STRING <details> <summary> Depends On </summary> workbookId </details> | SELECT |  | true |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| row | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> isTheFirstRowHeader, worksheetName, workbookId </details> | null |  | true |


#### JSON Example
```json
{
  "label" : "Append Row",
  "name" : "appendRow",
  "parameters" : {
    "workbookId" : "",
    "worksheetName" : "",
    "isTheFirstRowHeader" : false,
    "row" : { }
  },
  "type" : "microsoftExcel/v1/appendRow"
}
```


### Clear Worksheet
Name: clearWorksheet

Clear a worksheet of all values.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workbookId | Workbook ID | STRING | SELECT |  | true |
| worksheetName | Worksheet | STRING <details> <summary> Depends On </summary> workbookId </details> | SELECT |  | true |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |


#### JSON Example
```json
{
  "label" : "Clear Worksheet",
  "name" : "clearWorksheet",
  "parameters" : {
    "workbookId" : "",
    "worksheetName" : "",
    "isTheFirstRowHeader" : false
  },
  "type" : "microsoftExcel/v1/clearWorksheet"
}
```


### Delete Row
Name: deleteRow

Delete row on an existing sheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workbookId | Workbook ID | STRING | SELECT |  | true |
| worksheetName | Worksheet | STRING <details> <summary> Depends On </summary> workbookId </details> | SELECT |  | true |
| rowNumber | Row Number | INTEGER | INTEGER | The row number to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Row",
  "name" : "deleteRow",
  "parameters" : {
    "workbookId" : "",
    "worksheetName" : "",
    "rowNumber" : 1
  },
  "type" : "microsoftExcel/v1/deleteRow"
}
```


### Find Row by Number
Name: findRowByNum

Get row values from the worksheet by the row number.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workbookId | Workbook ID | STRING | SELECT |  | true |
| worksheetName | Worksheet | STRING <details> <summary> Depends On </summary> workbookId </details> | SELECT |  | true |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| rowNumber | Row Number | INTEGER | INTEGER | The row number to get the values from. | true |


#### JSON Example
```json
{
  "label" : "Find Row by Number",
  "name" : "findRowByNum",
  "parameters" : {
    "workbookId" : "",
    "worksheetName" : "",
    "isTheFirstRowHeader" : false,
    "rowNumber" : 1
  },
  "type" : "microsoftExcel/v1/findRowByNum"
}
```


### Update Row
Name: updateRow

Update a row in a worksheet.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workbookId | Workbook ID | STRING | SELECT |  | true |
| worksheetName | Worksheet | STRING <details> <summary> Depends On </summary> workbookId </details> | SELECT |  | true |
| rowNumber | Row Number | INTEGER | INTEGER | The row number to update. | true |
| isTheFirstRowHeader | Is the First Row Header? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If the first row is header. | true |
| row | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> isTheFirstRowHeader, worksheetName, workbookId </details> | null |  | true |


#### JSON Example
```json
{
  "label" : "Update Row",
  "name" : "updateRow",
  "parameters" : {
    "workbookId" : "",
    "worksheetName" : "",
    "rowNumber" : 1,
    "isTheFirstRowHeader" : false,
    "row" : { }
  },
  "type" : "microsoftExcel/v1/updateRow"
}
```




