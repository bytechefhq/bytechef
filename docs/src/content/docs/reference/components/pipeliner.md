---
title: "Pipeliner"
description: "Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting."
---

Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting.


Categories: crm


Type: pipeliner/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| spaceId | Space Id | STRING | Your Space ID | true |
| serverUrl | Server URL | STRING <details> <summary> Options </summary> https://us-east.api.pipelinersales.com/api/v100/rest/spaces/, https://eu-central.api.pipelinersales.com/api/v100/rest/spaces/, https://ca-central.api.pipelinersales.com/api/v100/rest/spaces/, https://ap-southeast.api.pipelinersales.com/api/v100/rest/spaces/ </details> |  | true |
| username | Username | STRING |  | true |
| password | Password | STRING |  | true |





<hr />



## Actions


### Create Account
Name: createAccount

Creates new account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Account | OBJECT <details> <summary> Properties </summary> {STRING\(owner_id), STRING\(name)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| success | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| data | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(owner_id), STRING\(name)} </details> |




#### JSON Example
```json
{
  "label" : "Create Account",
  "name" : "createAccount",
  "parameters" : {
    "__item" : {
      "owner_id" : "",
      "name" : ""
    }
  },
  "type" : "pipeliner/v1/createAccount"
}
```


### Create Contact
Name: createContact

Creates new Contact

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(owner_id), STRING\(first_name), STRING\(last_name)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| success | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| data | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(owner_id), STRING\(first_name), STRING\(last_name)} </details> |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
      "owner_id" : "",
      "first_name" : "",
      "last_name" : ""
    }
  },
  "type" : "pipeliner/v1/createContact"
}
```


### Create Task
Name: createTask

Creates new Task

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Task | OBJECT <details> <summary> Properties </summary> {STRING\(subject), STRING\(activity_type_id), STRING\(unit_id), STRING\(owner_id)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| success | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| data | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(subject), STRING\(activity_type_id), STRING\(unit_id), STRING\(owner_id)} </details> |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "__item" : {
      "subject" : "",
      "activity_type_id" : "",
      "unit_id" : "",
      "owner_id" : ""
    }
  },
  "type" : "pipeliner/v1/createTask"
}
```




