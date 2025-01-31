---
title: "Airtable"
description: "Airtable is a user-friendly and flexible cloud-based database management tool."
---
## Reference
<hr />

Airtable is a user-friendly and flexible cloud-based database management tool.


Categories: [productivity-and-collaboration]


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



## Triggers


### New Record
Trigger off when a new entry is added to the table that you have selected.

#### Type: POLLING
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Base ID | STRING | SELECT  |  ID of the base which contains the table that you want to monitor.  |
| Table | STRING | SELECT  |  The table to monitor for new records.  |
| Trigger Field | STRING | TEXT  |  It is essential to have a field for Created Time or Last Modified Time in your schema since this field is used to sort records, and the trigger will not function correctly without it. Therefore, if you don't have such a field in your schema, please create one.  |





<hr />



## Actions


### Create Record
Adds a record into an Airtable table.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Base ID | STRING | SELECT  |  ID of the base where table is located.  |
| Table ID | STRING | SELECT  |  The table where the record will be created.  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| DATE_TIME | DATE_TIME  |
| {} | OBJECT_BUILDER  |






