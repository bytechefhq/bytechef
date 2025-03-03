---
title: "MySQL"
description: "Query, insert and update data from MySQL."
---

Query, insert and update data from MySQL.



Type: mysql/v1

<hr />



## Connections

Version: 1


### custom

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING |  | true |
| password | Password | STRING |  | true |





<hr />



## Actions


### Query
Name: query

Execute an SQL query.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| query | Query | STRING | The raw SQL query to execute. You can use :property1 and :property2 in conjunction with parameters. | true |
| parameters | Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | The list of properties which should be used as query parameters. | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Query",
  "name" : "query",
  "parameters" : {
    "query" : "",
    "parameters" : { }
  },
  "type" : "mysql/v1/query"
}
```


### Insert
Name: insert

Insert rows in database.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| schema | Schema | STRING | Name of the schema the table belongs to. | true |
| table | Table | STRING | Name of the table in which to insert data to. | true |
| columns | Fields | ARRAY <details> <summary> Items </summary> [STRING] </details> | The list of the table field names where corresponding values would be inserted. | null |
| rows | Values | ARRAY <details> <summary> Items </summary> [{}] </details> | List of field values for corresponding field names | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Insert",
  "name" : "insert",
  "parameters" : {
    "schema" : "",
    "table" : "",
    "columns" : [ "" ],
    "rows" : [ { } ]
  },
  "type" : "mysql/v1/insert"
}
```


### Update
Name: update

Update rows in database.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| schema | Schema | STRING | Name of the schema the table belongs to. | true |
| table | Table | STRING | Name of the table in which to update data in. | true |
| columns | Fields | ARRAY <details> <summary> Items </summary> [STRING] </details> | The list of the table field names whose values would be updated. | null |
| updateKey | Update Key | STRING | The field name used as criteria to decide which rows in the database should be updated. | null |
| rows | Values | ARRAY <details> <summary> Items </summary> [{}] </details> | List of field values for corresponding field names. | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Update",
  "name" : "update",
  "parameters" : {
    "schema" : "",
    "table" : "",
    "columns" : [ "" ],
    "updateKey" : "",
    "rows" : [ { } ]
  },
  "type" : "mysql/v1/update"
}
```


### Delete
Name: delete

Delete rows from database.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| schema | Schema | STRING | Name of the schema the table belongs to. | true |
| table | Table | STRING | Name of the table in which to update data in. | true |
| deleteKey | Delete Key | STRING | Name of the field which decides which rows in the database should be deleted. | null |
| rows | Criteria Values | ARRAY <details> <summary> Items </summary> [{}] </details> | List of values that are used to test delete key. | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Delete",
  "name" : "delete",
  "parameters" : {
    "schema" : "",
    "table" : "",
    "deleteKey" : "",
    "rows" : [ { } ]
  },
  "type" : "mysql/v1/delete"
}
```


### Execute
Name: execute

Execute an SQL DML or DML statement.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| execute | Execute | STRING | The raw DML or DDL statement to execute. You can use :property1 and :property2 in conjunction with parameters. | true |
| columns | Fields to select | ARRAY <details> <summary> Items </summary> [{}] </details> | List of fields to select from. | null |
| parameters | Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | The list of values which should be used to replace corresponding criteria parameters. | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Execute",
  "name" : "execute",
  "parameters" : {
    "execute" : "",
    "columns" : [ { } ],
    "parameters" : { }
  },
  "type" : "mysql/v1/execute"
}
```




