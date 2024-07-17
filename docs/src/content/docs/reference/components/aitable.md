---
title: "AITable"
description: "AITable is an AI-powered platform that enables users to create interactive and dynamic tables for data visualization and analysis without requiring coding skills."
---
## Reference
<hr />

AITable is an AI-powered platform that enables users to create interactive and dynamic tables for data visualization and analysis without requiring coding skills.


Categories: [PRODUCTIVITY_AND_COLLABORATION]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |





<hr />





## Actions


### Find records
Find records in datasheet

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Space | STRING | SELECT  |  |
| Datasheet | STRING | SELECT  |  AITable Datasheet  |
| Field Names | [STRING] | ARRAY_BUILDER  |  The returned record results are limited to the specified fields.  |
| Record IDs | [STRING] | ARRAY_BUILDER  |  The IDs of the records to find.  |
| Max records | INTEGER | INTEGER  |  How many records are returned in total  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| {[{STRING(recordId), {}(fields)}](records)} | OBJECT_BUILDER  |
| STRING | TEXT  |





### Update record
Update record in datasheet

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Space | STRING | SELECT  |  |
| Datasheet | STRING | SELECT  |  AITable Datasheet  |
| Record | STRING | SELECT  |  Record to update  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| {[{STRING(recordId), {}(fields)}](records)} | OBJECT_BUILDER  |
| STRING | TEXT  |





### Create record
Creates a new record in datasheet.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Space | STRING | SELECT  |  |
| Datasheet | STRING | SELECT  |  AITable Datasheet  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| {[{STRING(recordId), {}(fields)}](records)} | OBJECT_BUILDER  |
| STRING | TEXT  |





<hr />

# Additional instructions
<hr />

## CONNECTION

[Setting up OAuth2](https://developers.aitable.ai/api/quick-start/#:~:text=API%20Token%20is%20the%20user,request%20to%20facilitate%20server%20authentication.)

[Guidejar](https://guidejar.com/guides/51781518-3dd5-4d75-9a37-0cc85a58a66f) tutorial.
