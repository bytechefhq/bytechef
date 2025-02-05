---
title: "PostgreSQL"
description: "Query, insert and update data from PostgreSQL."
---

Query, insert and update data from PostgreSQL.



Type: postgresql/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  | true  |
| password | Password | STRING | PASSWORD  |  | true  |





<hr />



## Actions


### Query
Execute an SQL query.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| query | Query | STRING | TEXT  |  The raw SQL query to execute. You can use :property1 and :property2 in conjunction with parameters.  |  true  |
| parameters | Parameters | {} | OBJECT_BUILDER  |  The list of properties which should be used as query parameters.  |  null  |




### Insert
Insert rows in database.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| schema | Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |  true  |
| table | Table | STRING | TEXT  |  Name of the table in which to insert data to.  |  true  |
| columns | Fields | [STRING] | ARRAY_BUILDER  |  The list of the table field names where corresponding values would be inserted.  |  null  |
| rows | Values | [{}] | ARRAY_BUILDER  |  List of field values for corresponding field names  |  null  |




### Update
Update rows in database.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| schema | Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |  true  |
| table | Table | STRING | TEXT  |  Name of the table in which to update data in.  |  true  |
| columns | Fields | [STRING] | ARRAY_BUILDER  |  The list of the table field names whose values would be updated.  |  null  |
| updateKey | Update Key | STRING | TEXT  |  The field name used as criteria to decide which rows in the database should be updated.  |  null  |
| rows | Values | [{}] | ARRAY_BUILDER  |  List of field values for corresponding field names.  |  null  |




### Delete
Delete rows from database.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| schema | Schema | STRING | TEXT  |  Name of the schema the table belongs to.  |  true  |
| table | Table | STRING | TEXT  |  Name of the table in which to update data in.  |  true  |
| deleteKey | Delete Key | STRING | TEXT  |  Name of the field which decides which rows in the database should be deleted.  |  null  |
| rows | Criteria Values | [{}] | ARRAY_BUILDER  |  List of values that are used to test delete key.  |  null  |




### Execute
Execute an SQL DML or DML statement.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| execute | Execute | STRING | TEXT  |  The raw DML or DDL statement to execute. You can use :property1 and :property2 in conjunction with parameters.  |  true  |
| columns | Fields to select | [{}] | ARRAY_BUILDER  |  List of fields to select from.  |  null  |
| parameters | Parameters | {} | OBJECT_BUILDER  |  The list of values which should be used to replace corresponding criteria parameters.  |  null  |






