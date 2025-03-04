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
| FIRST_NAME | First Name | STRING | The first name of the contact. | true |
| LAST_NAME | Last Name | STRING | The last name of the contact. | false |
| EMAIL_ADDRESS | Email Address | STRING | Email address of the contact. | false |
| PHONE | Phone | STRING | Phone number of the contact. | false |
| TITLE | Title | STRING | The contact's title in company. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| CONTACT_ID | INTEGER |  |
| FIRST_NAME | STRING |  |
| LAST_NAME | STRING |  |
| EMAIL_ADDRESS | STRING |  |
| PHONE | STRING |  |
| TITLE | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "FIRST_NAME" : "",
    "LAST_NAME" : "",
    "EMAIL_ADDRESS" : "",
    "PHONE" : "",
    "TITLE" : ""
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
| ORGANISATION_NAME | Organization Name | STRING | The name of the organization. | true |
| PHONE | Phone | STRING | A contact phone number for the organization. | false |
| WEBSITE | Website | STRING | The organization's website. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| ORGANISATION_ID | INTEGER |  |
| ORGANISATION_NAME | STRING |  |
| PHONE | STRING |  |
| WEBSITE | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Organization",
  "name" : "createOrganization",
  "parameters" : {
    "ORGANISATION_NAME" : "",
    "PHONE" : "",
    "WEBSITE" : ""
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
| TITLE | Title | STRING |  | true |
| STATUS | Status | STRING <details> <summary> Options </summary> Not Started, In Progress, Completed, Deferred, Waiting </details> | Task status | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| TASK_ID | INTEGER |  |
| TITLE | STRING |  |
| STATUS | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "TITLE" : "",
    "STATUS" : ""
  },
  "type" : "insightly/v1/createTask"
}
```




<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://crm.na1.insightly.com/Users/UserSettings)
