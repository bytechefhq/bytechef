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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| pod | Pod | STRING | TEXT  |  Your instances pod can be found under your API URL, e.g. https://api.{pod}.insightly.com/v3.1  |  true  |
| username | API Key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Contact
Creates new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {STRING\(FIRST_NAME), STRING\(LAST_NAME), STRING\(EMAIL_ADDRESS), STRING\(PHONE), STRING\(TITLE)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| CONTACT_ID | INTEGER | INTEGER  |
| FIRST_NAME | STRING | TEXT  |
| LAST_NAME | STRING | TEXT  |
| EMAIL_ADDRESS | STRING | TEXT  |
| PHONE | STRING | TEXT  |
| TITLE | STRING | TEXT  |






### Create Organization
Creates new organization.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Organization | {STRING\(ORGANISATION_NAME), STRING\(PHONE), STRING\(WEBSITE)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| ORGANISATION_ID | INTEGER | INTEGER  |
| ORGANISATION_NAME | STRING | TEXT  |
| PHONE | STRING | TEXT  |
| WEBSITE | STRING | TEXT  |






### Create Task
Creates new task.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Task | {STRING\(TITLE), STRING\(STATUS)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| TASK_ID | INTEGER | INTEGER  |
| TITLE | STRING | TEXT  |
| STATUS | STRING | TEXT  |








## Triggers



<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://crm.na1.insightly.com/Users/UserSettings)
