---
title: "Baserow"
description: "Baserow is an open-source, no-code database platform that enables users to create, manage, and collaborate on databases through a user-friendly interface."
---

Baserow is an open-source, no-code database platform that enables users to create, manage, and collaborate on databases through a user-friendly interface.


Categories: productivity-and-collaboration


Type: baserow/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Database Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Row
Creates a new row.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tableId | Table ID | INTEGER | INTEGER  |  ID of the table where the row must be created in.  |  true  |
| user_field_names | User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |  false  |
| fields | DYNAMIC_PROPERTIES | null  |




### Delete Row
Deletes the specified row.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tableId | Table ID | INTEGER | INTEGER  |  ID of the table containing the row to be deleted.  |  true  |
| rowId | Row ID | INTEGER | INTEGER  |  ID of the row to be deleted.  |  true  |




### Get Row
Fetches a single table row.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tableId | Table ID | INTEGER | INTEGER  |  ID of the table where you want to get the row from.  |  true  |
| rowId | Row ID | INTEGER | INTEGER  |  ID of the row to get.  |  true  |
| user_field_names | User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |  false  |




### List Rows
Lists table rows.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tableId | Table ID | INTEGER | INTEGER  |  ID of the table where you want to get the rows from.  |  true  |
| size | Size | INTEGER | INTEGER  |  The maximum number of rows to retrieve.  |  false  |
| order_by | Order By | STRING | TEXT  |  If provided rows will be order by specific field. Use - sign for descending ordering.  |  false  |
| user_field_names | User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |  false  |




### Update Row
Updates the specified row.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tableId | Table ID | INTEGER | INTEGER  |  ID of the table containing the row to be updated.  |  true  |
| rowId | Row ID | INTEGER | INTEGER  |  ID of the row to be updated.  |  true  |
| user_field_names | User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |  false  |
| fields | DYNAMIC_PROPERTIES | null  |






