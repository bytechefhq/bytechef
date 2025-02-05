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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| deployment | Deployment | STRING | TEXT  |  Actual deployment identifier or name to target a specific deployment within the Accelo platform.  |  true  |
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Company
Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Company | {STRING\(name), STRING\(website), STRING\(phone), STRING\(comments)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(id), STRING\(name)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} | OBJECT_BUILDER  |






### Create Contact
Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {STRING\(firstname), STRING\(surname), STRING\(company_id), STRING\(phone), STRING\(email)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(id), STRING\(firstname), STRING\(lastname), STRING\(email)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} | OBJECT_BUILDER  |






### Create Task
Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Title | STRING | TEXT  |  | true  |
| against_type | Against Type | STRING | SELECT  |  The type of object the task is against.  |  true  |
| against_id | Against Object ID | STRING | SELECT  |  ID of the object the task is against.  |  true  |
| date_started | Start Date | DATE | DATE  |  The date the task is is scheduled to start.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| response | {STRING\(id), STRING\(title)} | OBJECT_BUILDER  |
| meta | {STRING\(more_info), STRING\(status), STRING\(message)} | OBJECT_BUILDER  |








## Triggers



<hr />

