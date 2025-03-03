---
title: "Affinity"
description: "Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects."
---

Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects.


Categories: crm


Type: affinity/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| token | Token | STRING |  | true |





<hr />



## Actions


### Create Opportunity
Name: createOpportunity

Creates a new opportunity.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The name of the opportunity. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "Create Opportunity",
  "name" : "createOpportunity",
  "parameters" : {
    "name" : ""
  },
  "type" : "affinity/v1/createOpportunity"
}
```


### Create Organization
Name: createOrganization

Creates a new organization.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The name of the organization. | true |
| domain | Domain | STRING | The domain name of the organization. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| name | STRING |
| domain | STRING |




#### JSON Example
```json
{
  "label" : "Create Organization",
  "name" : "createOrganization",
  "parameters" : {
    "name" : "",
    "domain" : ""
  },
  "type" : "affinity/v1/createOrganization"
}
```


### Create Person
Name: createPerson

Creates a new person.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| first_name | First Name | STRING | The first name of the person. | true |
| last_name | Last Name | STRING | The last name of the person. | true |
| emails | Emails | ARRAY <details> <summary> Items </summary> [STRING] </details> | The email addresses of the person. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| first_name | STRING |
| last_name | STRING |
| emails | ARRAY <details> <summary> Items </summary> [STRING] </details> |




#### JSON Example
```json
{
  "label" : "Create Person",
  "name" : "createPerson",
  "parameters" : {
    "first_name" : "",
    "last_name" : "",
    "emails" : [ "" ]
  },
  "type" : "affinity/v1/createPerson"
}
```




