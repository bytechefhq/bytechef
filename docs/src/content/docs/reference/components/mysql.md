---
title: "MySQL"
description: "Query, insert and update data from MySQL."
---
## Reference
<hr />

Query, insert and update data from MySQL.



Version: 1

<hr />



## Connections

Version: 1

null



<hr />





## Actions


### Query
Execute an SQL query.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Query | STRING | TEXT  |
| Parameters | OBJECT | OBJECT_BUILDER  |




### Insert
Insert rows in database.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Schema | STRING | TEXT  |
| Table | STRING | TEXT  |
| Columns | ARRAY | ARRAY_BUILDER  |
| Rows | ARRAY | ARRAY_BUILDER  |




### Update
Update rows in database.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Schema | STRING | TEXT  |
| Table | STRING | TEXT  |
| Columns | ARRAY | ARRAY_BUILDER  |
| Update Key | STRING | TEXT  |
| Rows | ARRAY | ARRAY_BUILDER  |




### Delete
Delete rows from database.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Schema | STRING | TEXT  |
| Table | STRING | TEXT  |
| Update Key | STRING | TEXT  |
| Rows | ARRAY | ARRAY_BUILDER  |




### Execute
Execute an SQL DML or DML statement.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Execute | STRING | TEXT  |
| Rows | ARRAY | ARRAY_BUILDER  |
| Parameters | OBJECT | OBJECT_BUILDER  |




