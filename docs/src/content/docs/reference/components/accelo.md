---
title: "Accelo"
description: "Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system."
---
## Reference
<hr />

Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system.


Categories: [crm, project-management]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Deployment | STRING | TEXT  |  Actual deployment identifier or name to target a specific deployment within the Accelo platform.  |
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create Company
Creates a new company.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company | {STRING\(name), STRING\(website), STRING\(phone), STRING\(comments)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING\(id), STRING\(name)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} | OBJECT_BUILDER  |






### Create Contact
Creates a new contact.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {STRING\(firstname), STRING\(surname), STRING\(company_id), STRING\(phone), STRING\(email)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING\(id), STRING\(firstname), STRING\(lastname), STRING\(email)}\(response), {STRING\(more_info), STRING\(status), STRING\(message)}\(meta)} | OBJECT_BUILDER  |






### Create Task
Creates a new task.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Title | STRING | TEXT  |  |
| Against Type | STRING | SELECT  |  The type of object the task is against.  |
| Against Object ID | STRING | SELECT  |  ID of the object the task is against.  |
| Start Date | DATE | DATE  |  The date the task is is scheduled to start.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(title)} | OBJECT_BUILDER  |
| {STRING\(more_info), STRING\(status), STRING\(message)} | OBJECT_BUILDER  |






