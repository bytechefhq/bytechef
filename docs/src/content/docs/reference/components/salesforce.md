---
title: "Salesforce"
description: "Salesforce is a cloud-based customer relationship management (CRM) platform that provides tools for sales, service, marketing, and analytics to help businesses manage customer interactions and data."
---

Salesforce is a cloud-based customer relationship management (CRM) platform that provides tools for sales, service, marketing, and analytics to help businesses manage customer interactions and data.


Categories: crm


Type: salesforce/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| subdomain | Subdomain | STRING | The subdomain of your Salesforce instance. | true |
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Record
Name: createRecord

Creates a new record of a specified Salesforce object.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| object | Salesforce Object | STRING |  | true |
| fields | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> object </details> |  | true |
| customFields | Custom Fields | OBJECT <details> <summary> Properties </summary> {} </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |




#### JSON Example
```json
{
  "label" : "Create Record",
  "name" : "createRecord",
  "parameters" : {
    "object" : "",
    "fields" : { },
    "customFields" : { }
  },
  "type" : "salesforce/v1/createRecord"
}
```


### Delete Record
Name: deleteRecord

Deletes an existing record of a specified Salesforce object.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| object | Salesforce Object | STRING |  | true |
| id | Record ID | STRING <details> <summary> Depends On </summary> object </details> | ID of the object to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Record",
  "name" : "deleteRecord",
  "parameters" : {
    "object" : "",
    "id" : ""
  },
  "type" : "salesforce/v1/deleteRecord"
}
```


### SOQL Query
Name: soqlQuery

Executes a raw SOQL query to  extract data from Salesforce.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| q | Query | STRING | SOQL query to execute. | true |


#### JSON Example
```json
{
  "label" : "SOQL Query",
  "name" : "soqlQuery",
  "parameters" : {
    "q" : ""
  },
  "type" : "salesforce/v1/soqlQuery"
}
```


### Update Record
Name: updateRecord

Updates an existing record for a specified Salesforce object.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| object | Salesforce Object | STRING |  | true |
| id | Record ID | STRING <details> <summary> Depends On </summary> object </details> | ID of the record to update. | true |
| fields | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> object </details> |  | true |
| customFields | Custom Fields | OBJECT <details> <summary> Properties </summary> {} </details> |  | false |


#### JSON Example
```json
{
  "label" : "Update Record",
  "name" : "updateRecord",
  "parameters" : {
    "object" : "",
    "id" : "",
    "fields" : { },
    "customFields" : { }
  },
  "type" : "salesforce/v1/updateRecord"
}
```




## Triggers


### New Record
Name: newRecord

Triggers when there is new record in Salesforce.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| object | Salesforce Object | STRING |  | true |


#### JSON Example
```json
{
  "label" : "New Record",
  "name" : "newRecord",
  "parameters" : {
    "object" : ""
  },
  "type" : "salesforce/v1/newRecord"
}
```


### Updated Record
Name: updatedRecord

Triggers when record is updated.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| object | Salesforce Object | STRING |  | true |


#### JSON Example
```json
{
  "label" : "Updated Record",
  "name" : "updatedRecord",
  "parameters" : {
    "object" : ""
  },
  "type" : "salesforce/v1/updatedRecord"
}
```


<hr />

