---
title: "Accelo"
description: "Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system."
---

Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system.


Categories: crm, project-management


Type: accelo/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| deployment | Deployment | STRING | Actual deployment identifier or name to target a specific deployment within the Accelo platform. | true |
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Company | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(website), STRING\(phone), STRING\(comments)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(name)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} </details> |




#### JSON Example
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "__item" : {
      "name" : "",
      "website" : "",
      "phone" : "",
      "comments" : ""
    }
  },
  "type" : "accelo/v1/createCompany"
}
```


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(firstname), STRING\(surname), STRING\(company_id), STRING\(phone), STRING\(email)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(firstname), STRING\(lastname), STRING\(email)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} </details> |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
      "firstname" : "",
      "surname" : "",
      "company_id" : "",
      "phone" : "",
      "email" : ""
    }
  },
  "type" : "accelo/v1/createContact"
}
```


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| title | Title | STRING |  | true |
| against_type | Against Type | STRING <details> <summary> Options </summary> company, prospect </details> | The type of object the task is against. | true |
| against_id | Against Object ID | STRING <details> <summary> Depends On </summary> against_type </details> | ID of the object the task is against. | true |
| date_started | Start Date | DATE | The date the task is is scheduled to start. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| response | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(title)} </details> |
| meta | OBJECT <details> <summary> Properties </summary> {STRING\(more_info), STRING\(status), STRING\(message)} </details> |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "title" : "",
    "against_type" : "",
    "against_id" : "",
    "date_started" : "2021-01-01"
  },
  "type" : "accelo/v1/createTask"
}
```




