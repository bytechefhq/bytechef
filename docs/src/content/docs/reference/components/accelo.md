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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| deployment | Deployment | STRING | TEXT | Actual deployment identifier or name to target a specific deployment within the Accelo platform. | true |
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Company | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(website), STRING\(phone), STRING\(comments)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(name)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(firstname), STRING\(surname), STRING\(company_id), STRING\(phone), STRING\(email)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(firstname), STRING\(lastname), STRING\(email)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| title | Title | STRING | TEXT |  | true |
| against_type | Against Type | STRING <details> <summary> Options </summary> company, prospect </details> | SELECT | The type of object the task is against. | true |
| against_id | Against Object ID | STRING <details> <summary> Depends On </summary> against_type </details> | SELECT | ID of the object the task is against. | true |
| date_started | Start Date | DATE | DATE | The date the task is is scheduled to start. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| response | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(title)} </details> | OBJECT_BUILDER |
| meta | OBJECT <details> <summary> Properties </summary> {STRING\(more_info), STRING\(status), STRING\(message)} </details> | OBJECT_BUILDER |




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




