---
title: "Snowflake"
description: "Snowflake enables organizations to collaborate, build AI-powered data apps, and unlock data insights—all within a secure and scalable AI Data Cloud."
---

Snowflake enables organizations to collaborate, build AI-powered data apps, and unlock data insights—all within a secure and scalable AI Data Cloud.


Categories: Analytics


Type: snowflake/v1

<hr />



## Connections

Version: 1


### oauth2_authorization_code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| account_identifier | null | STRING | Account identifier. | true |
| clientId | null | STRING | Snowflake OAuth Client ID. | true |
| clientSecret | null | STRING | Snowflake OAuth Client Secret. | true |





<hr />



## Actions


### Delete Row
Name: deleteRow

Delete row from the table.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| database | Database | STRING |  | true |
| schema | Schema | STRING <details> <summary> Depends On </summary> database </details> |  | true |
| table | Table | STRING <details> <summary> Depends On </summary> schema </details> |  | true |
| column | Column | STRING | Column name that will be checked for condition. | true |
| condition | Condition | STRING | Condition that will be checked in the column. | true |

#### Example JSON Structure
```json
{
  "label" : "Delete Row",
  "name" : "deleteRow",
  "parameters" : {
    "database" : "",
    "schema" : "",
    "table" : "",
    "column" : "",
    "condition" : ""
  },
  "type" : "snowflake/v1/deleteRow"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| resultSetMetaData | OBJECT <details> <summary> Properties </summary> {INTEGER\(numRows), STRING\(format), [STRING\($name), STRING\($database), STRING\($schema), STRING\($table), {}\($scale), {}\($precision), INTEGER\($length), STRING\($type), BOOLEAN\($nullable), INTEGER\($byteLength), {}\($collation)]\(rowType), [INTEGER\($rowCount), INTEGER\($uncompressedSize)]\(partitionInfo)} </details> |  |
| data | ARRAY <details> <summary> Items </summary> [] </details> |  |
| code | STRING |  |
| statementStatusUrl | STRING |  |
| sqlState | STRING |  |
| statementHandle | STRING |  |
| message | STRING |  |
| createdOn | DATE |  |
| stats | ARRAY <details> <summary> Items </summary> [] </details> |  |




#### Output Example
```json
{
  "resultSetMetaData" : {
    "numRows" : 1,
    "format" : "",
    "rowType" : [ "", "", "", "", { }, { }, 1, "", false, 1, { } ],
    "partitionInfo" : [ 1, 1 ]
  },
  "data" : [ ],
  "code" : "",
  "statementStatusUrl" : "",
  "sqlState" : "",
  "statementHandle" : "",
  "message" : "",
  "createdOn" : "2021-01-01",
  "stats" : [ ]
}
```


### Execute SQL
Name: executeSql

Execute SQL statement.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| statement | Statement | STRING | SQL statement that will be executed. | true |

#### Example JSON Structure
```json
{
  "label" : "Execute SQL",
  "name" : "executeSql",
  "parameters" : {
    "statement" : ""
  },
  "type" : "snowflake/v1/executeSql"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| resultSetMetaData | OBJECT <details> <summary> Properties </summary> {INTEGER\(numRows), STRING\(format), [STRING\($name), STRING\($database), STRING\($schema), STRING\($table), {}\($scale), {}\($precision), INTEGER\($length), STRING\($type), BOOLEAN\($nullable), INTEGER\($byteLength), {}\($collation)]\(rowType), [INTEGER\($rowCount), INTEGER\($uncompressedSize)]\(partitionInfo)} </details> |  |
| data | ARRAY <details> <summary> Items </summary> [] </details> |  |
| code | STRING |  |
| statementStatusUrl | STRING |  |
| sqlState | STRING |  |
| statementHandle | STRING |  |
| message | STRING |  |
| createdOn | DATE |  |
| stats | ARRAY <details> <summary> Items </summary> [] </details> |  |




#### Output Example
```json
{
  "resultSetMetaData" : {
    "numRows" : 1,
    "format" : "",
    "rowType" : [ "", "", "", "", { }, { }, 1, "", false, 1, { } ],
    "partitionInfo" : [ 1, 1 ]
  },
  "data" : [ ],
  "code" : "",
  "statementStatusUrl" : "",
  "sqlState" : "",
  "statementHandle" : "",
  "message" : "",
  "createdOn" : "2021-01-01",
  "stats" : [ ]
}
```


### Insert Row
Name: insertRow

Insert row into the table.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| database | Database | STRING |  | true |
| schema | Schema | STRING <details> <summary> Depends On </summary> database </details> |  | true |
| table | Table | STRING <details> <summary> Depends On </summary> schema </details> |  | true |
| values | Values | STRING | Values to insert into the table. Seperated by comma. | true |

#### Example JSON Structure
```json
{
  "label" : "Insert Row",
  "name" : "insertRow",
  "parameters" : {
    "database" : "",
    "schema" : "",
    "table" : "",
    "values" : ""
  },
  "type" : "snowflake/v1/insertRow"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| resultSetMetaData | OBJECT <details> <summary> Properties </summary> {INTEGER\(numRows), STRING\(format), [STRING\($name), STRING\($database), STRING\($schema), STRING\($table), {}\($scale), {}\($precision), INTEGER\($length), STRING\($type), BOOLEAN\($nullable), INTEGER\($byteLength), {}\($collation)]\(rowType), [INTEGER\($rowCount), INTEGER\($uncompressedSize)]\(partitionInfo)} </details> |  |
| data | ARRAY <details> <summary> Items </summary> [] </details> |  |
| code | STRING |  |
| statementStatusUrl | STRING |  |
| sqlState | STRING |  |
| statementHandle | STRING |  |
| message | STRING |  |
| createdOn | DATE |  |
| stats | ARRAY <details> <summary> Items </summary> [] </details> |  |




#### Output Example
```json
{
  "resultSetMetaData" : {
    "numRows" : 1,
    "format" : "",
    "rowType" : [ "", "", "", "", { }, { }, 1, "", false, 1, { } ],
    "partitionInfo" : [ 1, 1 ]
  },
  "data" : [ ],
  "code" : "",
  "statementStatusUrl" : "",
  "sqlState" : "",
  "statementHandle" : "",
  "message" : "",
  "createdOn" : "2021-01-01",
  "stats" : [ ]
}
```


### Update Row
Name: updateRow

Update row from the table.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| database | Database | STRING |  | true |
| schema | Schema | STRING <details> <summary> Depends On </summary> database </details> |  | true |
| table | Table | STRING <details> <summary> Depends On </summary> schema </details> |  | true |
| column | Column | STRING | Column name that will be checked for condition. | true |
| condition | Condition | STRING | Condition that will be checked in the column. | true |
| values | Values | STRING | Updated values of the table. Seperated by comma. | true |

#### Example JSON Structure
```json
{
  "label" : "Update Row",
  "name" : "updateRow",
  "parameters" : {
    "database" : "",
    "schema" : "",
    "table" : "",
    "column" : "",
    "condition" : "",
    "values" : ""
  },
  "type" : "snowflake/v1/updateRow"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| resultSetMetaData | OBJECT <details> <summary> Properties </summary> {INTEGER\(numRows), STRING\(format), [STRING\($name), STRING\($database), STRING\($schema), STRING\($table), {}\($scale), {}\($precision), INTEGER\($length), STRING\($type), BOOLEAN\($nullable), INTEGER\($byteLength), {}\($collation)]\(rowType), [INTEGER\($rowCount), INTEGER\($uncompressedSize)]\(partitionInfo)} </details> |  |
| data | ARRAY <details> <summary> Items </summary> [] </details> |  |
| code | STRING |  |
| statementStatusUrl | STRING |  |
| sqlState | STRING |  |
| statementHandle | STRING |  |
| message | STRING |  |
| createdOn | DATE |  |
| stats | ARRAY <details> <summary> Items </summary> [] </details> |  |




#### Output Example
```json
{
  "resultSetMetaData" : {
    "numRows" : 1,
    "format" : "",
    "rowType" : [ "", "", "", "", { }, { }, 1, "", false, 1, { } ],
    "partitionInfo" : [ 1, 1 ]
  },
  "data" : [ ],
  "code" : "",
  "statementStatusUrl" : "",
  "sqlState" : "",
  "statementHandle" : "",
  "message" : "",
  "createdOn" : "2021-01-01",
  "stats" : [ ]
}
```




<hr />

# Additional instructions
<hr />

