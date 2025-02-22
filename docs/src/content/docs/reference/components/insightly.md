---
title: "Insightly"
description: "Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform."
---

Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform.


Categories: crm


Type: insightly/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| pod | Pod | STRING | Your instances pod can be found under your API URL, e.g. https://api.{pod}.insightly.com/v3.1 | true |
| username | API Key | STRING |  | true |





<hr />



## Actions


### Create Contact
Name: createContact

Creates new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(FIRST_NAME), STRING\(LAST_NAME), STRING\(EMAIL_ADDRESS), STRING\(PHONE), STRING\(TITLE)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| CONTACT_ID | INTEGER |
| FIRST_NAME | STRING |
| LAST_NAME | STRING |
| EMAIL_ADDRESS | STRING |
| PHONE | STRING |
| TITLE | STRING |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
      "FIRST_NAME" : "",
      "LAST_NAME" : "",
      "EMAIL_ADDRESS" : "",
      "PHONE" : "",
      "TITLE" : ""
    }
  },
  "type" : "insightly/v1/createContact"
}
```


### Create Organization
Name: createOrganization

Creates new organization.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Organization | OBJECT <details> <summary> Properties </summary> {STRING\(ORGANISATION_NAME), STRING\(PHONE), STRING\(WEBSITE)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| ORGANISATION_ID | INTEGER |
| ORGANISATION_NAME | STRING |
| PHONE | STRING |
| WEBSITE | STRING |




#### JSON Example
```json
{
  "label" : "Create Organization",
  "name" : "createOrganization",
  "parameters" : {
    "__item" : {
      "ORGANISATION_NAME" : "",
      "PHONE" : "",
      "WEBSITE" : ""
    }
  },
  "type" : "insightly/v1/createOrganization"
}
```


### Create Task
Name: createTask

Creates new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Task | OBJECT <details> <summary> Properties </summary> {STRING\(TITLE), STRING\(STATUS)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| TASK_ID | INTEGER |
| TITLE | STRING |
| STATUS | STRING |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "__item" : {
      "TITLE" : "",
      "STATUS" : ""
    }
  },
  "type" : "insightly/v1/createTask"
}
```




<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://crm.na1.insightly.com/Users/UserSettings)
