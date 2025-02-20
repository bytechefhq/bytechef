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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| pod | Pod | STRING | TEXT | Your instances pod can be found under your API URL, e.g. https://api.{pod}.insightly.com/v3.1 | true |
| username | API Key | STRING | TEXT |  | true |





<hr />



## Actions


### Create Contact
Name: createContact

Creates new contact.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(FIRST_NAME), STRING\(LAST_NAME), STRING\(EMAIL_ADDRESS), STRING\(PHONE), STRING\(TITLE)} </details> | OBJECT_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| CONTACT_ID | INTEGER | INTEGER |
| FIRST_NAME | STRING | TEXT |
| LAST_NAME | STRING | TEXT |
| EMAIL_ADDRESS | STRING | TEXT |
| PHONE | STRING | TEXT |
| TITLE | STRING | TEXT |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Organization | OBJECT <details> <summary> Properties </summary> {STRING\(ORGANISATION_NAME), STRING\(PHONE), STRING\(WEBSITE)} </details> | OBJECT_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| ORGANISATION_ID | INTEGER | INTEGER |
| ORGANISATION_NAME | STRING | TEXT |
| PHONE | STRING | TEXT |
| WEBSITE | STRING | TEXT |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Task | OBJECT <details> <summary> Properties </summary> {STRING\(TITLE), STRING\(STATUS)} </details> | OBJECT_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| TASK_ID | INTEGER | INTEGER |
| TITLE | STRING | TEXT |
| STATUS | STRING | TEXT |




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
