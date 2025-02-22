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
| __item | Opportunity | OBJECT <details> <summary> Properties </summary> {STRING\(name)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |




#### JSON Example
```json
{
  "label" : "Create Opportunity",
  "name" : "createOpportunity",
  "parameters" : {
    "__item" : {
      "name" : ""
    }
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
| __item | Organization | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(domain)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(domain)} </details> |




#### JSON Example
```json
{
  "label" : "Create Organization",
  "name" : "createOrganization",
  "parameters" : {
    "__item" : {
      "name" : "",
      "domain" : ""
    }
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
| __item | Person | OBJECT <details> <summary> Properties </summary> {STRING\(first_name), STRING\(last_name), [STRING]\(emails)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(first_name), STRING\(last_name), [STRING]\(emails)} </details> |




#### JSON Example
```json
{
  "label" : "Create Person",
  "name" : "createPerson",
  "parameters" : {
    "__item" : {
      "first_name" : "",
      "last_name" : "",
      "emails" : [ "" ]
    }
  },
  "type" : "affinity/v1/createPerson"
}
```




