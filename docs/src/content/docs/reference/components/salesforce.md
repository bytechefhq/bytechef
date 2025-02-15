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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| subdomain | Subdomain | STRING | TEXT  |  The subdomain of your Salesforce instance.  |  true  |
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Record
Name: createRecord

Creates a new record of a specified Salesforce object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| object | Salesforce Object | STRING | SELECT  |  | true  |
| fields | DYNAMIC_PROPERTIES | null  |
| customFields | Custom Fields | {} | OBJECT_BUILDER  |  | false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |






### Delete Record
Name: deleteRecord

Deletes an existing record of a specified Salesforce object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| object | Salesforce Object | STRING | SELECT  |  | true  |
| id | Record ID | STRING | SELECT  |  ID of the object to delete.  |  true  |




### SOQL Query
Name: soqlQuery

Executes a raw SOQL query to  extract data from Salesforce.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| q | Query | STRING | TEXT_AREA  |  SOQL query to execute.  |  true  |




### Update Record
Name: updateRecord

Updates an existing record for a specified Salesforce object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| object | Salesforce Object | STRING | SELECT  |  | true  |
| id | Record ID | STRING | SELECT  |  ID of the record to update.  |  true  |
| fields | DYNAMIC_PROPERTIES | null  |
| customFields | Custom Fields | {} | OBJECT_BUILDER  |  | false  |






