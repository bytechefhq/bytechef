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


### null

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  |
| Password | STRING | PASSWORD  |  |





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
| Fields | [STRING] | ARRAY_BUILDER  |  The list of the table field names where corresponding values would be inserted.  |
| Values | [{}] | ARRAY_BUILDER  |  List of field values for corresponding field names  |




### Update
Update rows in database.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |
| Table | STRING | TEXT  |  Name of the table in which to update data in.  |
| Fields | [STRING] | ARRAY_BUILDER  |  The list of the table field names whose values would be updated.  |
| Update Key | STRING | TEXT  |  The field name used as criteria to decide which rows in the database should be updated.  |
| Values | [{}] | ARRAY_BUILDER  |  List of field values for corresponding field names.  |




### Delete
Delete rows from database.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |
| Table | STRING | TEXT  |  Name of the table in which to update data in.  |
| Delete Key | STRING | TEXT  |  Name of the field which decides which rows in the database should be deleted.  |
| Criteria Values | [{}] | ARRAY_BUILDER  |  List of values that are used to test delete key.  |




### Execute
Execute an SQL DML or DML statement.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Execute | STRING | TEXT  |  The raw DML or DDL statement to execute. You can use :property1 and :property2 in conjunction with parameters.  |
| Fields to select | [{}] | ARRAY_BUILDER  |  List of fields to select from.  |
| Parameters | {} | OBJECT_BUILDER  |  The list of values which should be used to replace corresponding criteria parameters.  |




