---
title: "PostgreSQL"
description: "Query, insert and update data from PostgreSQL."
---
## Reference
<hr />

Query, insert and update data from PostgreSQL.



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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Query | STRING | TEXT  |  The raw SQL query to execute. You can use :property1 and :property2 in conjunction with parameters.  |
| Parameters | {} | OBJECT_BUILDER  |  The list of properties which should be used as query parameters.  |




### Insert
Insert rows in database.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |
| Table | STRING | TEXT  |  Name of the table in which to insert data to.  |
| Columns | [STRING] | ARRAY_BUILDER  |  The list of the properties which should used as columns for the new rows.  |
| Rows | [{}] | ARRAY_BUILDER  |  List of rows.  |




### Update
Update rows in database.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |
| Table | STRING | TEXT  |  Name of the table in which to update data in.  |
| Columns | [STRING] | ARRAY_BUILDER  |  The list of the properties which should used as columns for the updated rows.  |
| Update Key | STRING | TEXT  |  The name of the property which decides which rows in the database should be updated.  |
| Rows | [{}] | ARRAY_BUILDER  |  List of rows.  |




### Delete
Delete rows from database.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |
| Table | STRING | TEXT  |  Name of the table in which to update data in.  |
| Update Key | STRING | TEXT  |  Name of the property which decides which rows in the database should be deleted.  |
| Rows | [{}] | ARRAY_BUILDER  |  List of rows.  |




### Execute
Execute an SQL DML or DML statement.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Execute | STRING | TEXT  |  The raw DML or DDL statement to execute. You can use :property1 and :property2 in conjunction with parameters.  |
| Rows | [{}] | ARRAY_BUILDER  |  List of rows.  |
| Parameters | {} | OBJECT_BUILDER  |  The list of properties which should be used as parameters.  |




