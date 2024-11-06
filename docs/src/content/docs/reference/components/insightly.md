---
title: "Insightly"
description: "Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform."
---
## Reference
<hr />

Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform.


Categories: [crm]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Pod | STRING | TEXT  |  Your instances pod can be found under your API URL, e.g. https://api.{pod}.insightly.com/v3.1  |
| API Key | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create Contact
Creates new contact.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {STRING\(FIRST_NAME), STRING\(LAST_NAME), STRING\(EMAIL_ADDRESS), STRING\(PHONE), STRING\(TITLE)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create Organization
Creates new organization.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Organization | {STRING\(ORGANISATION_NAME), STRING\(PHONE), STRING\(WEBSITE)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create Task
Creates new task.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | {STRING\(TITLE), STRING\(STATUS)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |






<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://crm.na1.insightly.com/Users/UserSettings)
