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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |





<hr />





## Actions


### Find records
Find records in datasheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Space | STRING | SELECT  |
| Datasheet | STRING | SELECT  |
| Field Names | ARRAY | ARRAY_BUILDER  |
| Record IDs | ARRAY | ARRAY_BUILDER  |
| Max records | INTEGER | INTEGER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |





### Update record
Update record in datasheet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Space | STRING | SELECT  |
| Datasheet | STRING | SELECT  |
| Record | STRING | SELECT  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |





### Create record
Creates a new record in datasheet.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Space | STRING | SELECT  |
| Datasheet | STRING | SELECT  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |





