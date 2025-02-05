---
title: "Airtable"
description: "Airtable is a user-friendly and flexible cloud-based database management tool."
---

Airtable is a user-friendly and flexible cloud-based database management tool.


Categories: productivity-and-collaboration


Type: airtable/v1

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
Adds a record into an Airtable table.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| baseId | Base ID | STRING | SELECT  |  ID of the base where table is located.  |  true  |
| tableId | Table ID | STRING | SELECT  |  The table where the record will be created.  |  true  |
| __item | DYNAMIC_PROPERTIES | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| createdTime | DATE_TIME | DATE_TIME  |
| fields | {} | OBJECT_BUILDER  |








## Triggers


### New Record
Trigger off when a new entry is added to the table that you have selected.

Type: POLLING
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| baseId | Base ID | STRING | SELECT  |  ID of the base which contains the table that you want to monitor.  |  true  |
| tableId | Table | STRING | SELECT  |  The table to monitor for new records.  |  true  |
| triggerField | Trigger Field | STRING | TEXT  |  It is essential to have a field for Created Time or Last Modified Time in your schema since this field is used to sort records, and the trigger will not function correctly without it. Therefore, if you don't have such a field in your schema, please create one.  |  true  |





<hr />

