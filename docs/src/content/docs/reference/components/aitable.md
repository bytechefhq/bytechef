---
title: "AITable"
description: "AITable is an AI-powered platform that enables users to create interactive and dynamic tables for data visualization and analysis without requiring coding skills."
---

AITable is an AI-powered platform that enables users to create interactive and dynamic tables for data visualization and analysis without requiring coding skills.


Categories: productivity-and-collaboration


Type: aitable/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Record
Creates a new record in datasheet.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spaceId | Space ID | STRING | SELECT  |  | true  |
| datasheetId | Datasheet ID | STRING | SELECT  |  AITable Datasheet ID  |  true  |
| fields | DYNAMIC_PROPERTIES | null  |




### Find Records
Find records in datasheet

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spaceId | Space ID | STRING | SELECT  |  | true  |
| datasheetId | Datasheet ID | STRING | SELECT  |  AITable Datasheet ID  |  true  |
| fields | Field Names | [STRING] | ARRAY_BUILDER  |  The returned record results are limited to the specified fields.  |  false  |
| recordIds | Record IDs | [STRING] | ARRAY_BUILDER  |  The IDs of the records to find.  |  false  |
| maxRecords | Max Records | INTEGER | INTEGER  |  How many records are returned in total  |  false  |




### Update Record
Update record in datasheet

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spaceId | Space ID | STRING | SELECT  |  | true  |
| datasheetId | Datasheet ID | STRING | SELECT  |  AITable Datasheet ID  |  true  |
| recordId | Record ID | STRING | SELECT  |  ID of the record to update.  |  true  |
| fields | DYNAMIC_PROPERTIES | null  |






<hr />

# Additional instructions
<hr />

## CONNECTION

[Setting up OAuth2](https://developers.aitable.ai/api/quick-start/#:~:text=API%20Token%20is%20the%20user,request%20to%20facilitate%20server%20authentication.)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.69531250% + 32px)"><iframe src="https://www.guidejar.com/embed/51781518-3dd5-4d75-9a37-0cc85a58a66f?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
