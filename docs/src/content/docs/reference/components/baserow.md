---
title: "Baserow"
description: "Baserow is an open-source, no-code database platform that enables users to create, manage, and collaborate on databases through a user-friendly interface."
---
## Reference
<hr />

Baserow is an open-source, no-code database platform that enables users to create, manage, and collaborate on databases through a user-friendly interface.


Categories: [productivity-and-collaboration]


Version: 1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Database Token | STRING | TEXT  |  |





<hr />





## Actions


### Create Row
Creates a new row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table ID | INTEGER | INTEGER  |  ID of the table where the row must be created in.  |
| User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |






### Delete Row
Deletes the specified row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table ID | INTEGER | INTEGER  |  ID of the table containing the row to be deleted.  |
| Row ID | INTEGER | INTEGER  |  ID of the row to be deleted.  |




### Get Row
Fetches a single table row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table ID | INTEGER | INTEGER  |  ID of the table where you want to get the row from.  |
| Row ID | INTEGER | INTEGER  |  ID of the row to get.  |
| User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |




### List Rows
Lists table rows.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table ID | INTEGER | INTEGER  |  ID of the table where you want to get the rows from.  |
| Size | INTEGER | INTEGER  |  The maximum number of rows to retrieve.  |
| Order By | STRING | TEXT  |  If provided rows will be order by specific field. Use - sign for descending ordering.  |
| User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |




### Update Row
Updates the specified row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table ID | INTEGER | INTEGER  |  ID of the table containing the row to be updated.  |
| Row ID | INTEGER | INTEGER  |  ID of the row to be updated.  |
| User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |






