---
title: "Airtable"
description: "Airtable is a user-friendly and flexible cloud-based database management tool."
---
## Reference
<hr />

Airtable is a user-friendly and flexible cloud-based database management tool.

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



## Triggers


### New Record
Trigger off when a new entry is added to the table that you have selected.

#### Type: POLLING
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| BaseId | STRING | SELECT  |
| TableId | STRING | SELECT  |
| TriggerField | STRING | TEXT  |





<hr />



## Actions


### Creates a record
Adds a record into an Airtable table.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Base Id | STRING | SELECT  |
| Table Id | STRING | SELECT  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| DATE_TIME | DATE_TIME  |
| OBJECT | OBJECT_BUILDER  |





