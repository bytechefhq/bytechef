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
Creates a new Projects row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table | INTEGER | INTEGER  |  Table where the row must be created in.  |
| User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |






### Update Row
Updates the specified row.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Table | INTEGER | INTEGER  |  Table containing the row to be updated.  |
| Row | INTEGER | INTEGER  |  The row to be updated.  |
| User Field Names | BOOLEAN | SELECT  |  The field names returned by this endpoint will be the actual names of the fields.  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |






