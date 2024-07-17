---
title: "Accelo"
description: "Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system."
---
## Reference
<hr />

Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system.


Categories: [CRM, PROJECT_MANAGEMENT]


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





## Actions


### Create company
Creates a new company

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name of the company  |
| Website | STRING | TEXT  |  The company's website.  |
| Phone | STRING | TEXT  |  A contact phone number for the company.  |
| Comments | STRING | TEXT  |  Any comments or notes made against the company.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(name)} | OBJECT_BUILDER  |
| {STRING(more_info), STRING(status), STRING(message)} | OBJECT_BUILDER  |





### Create contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First name | STRING | TEXT  |  The firstname of the contact.  |
| Last name | STRING | TEXT  |  The lastname of the contact.  |
| Company | STRING | SELECT  |  This is the company the new affiliated contact will be associated with.  |
| Phone | STRING | TEXT  |  The contact's phone number in their role in the associated company.  |
| Email | STRING | EMAIL  |  The contact's email address.  |
| Position | STRING | TEXT  |  The contact's position in the associated company.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(firstname), STRING(surname), STRING(email)} | OBJECT_BUILDER  |
| {STRING(more_info), STRING(status), STRING(message)} | OBJECT_BUILDER  |





### Create task
Creates a new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Title | STRING | TEXT  |  |
| Against type | STRING | SELECT  |  The type of object the task is against.  |
| Against object | STRING | SELECT  |  Object the task is against.  |
| Start date | DATE | DATE  |  The date the task is is scheduled to start.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(title)} | OBJECT_BUILDER  |
| {STRING(more_info), STRING(status), STRING(message)} | OBJECT_BUILDER  |





