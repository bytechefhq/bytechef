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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Deployment | STRING | TEXT  |
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />





## Actions


### Create company
Creates a new company

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Name | STRING | TEXT  |
| Website | STRING | TEXT  |
| Phone | STRING | TEXT  |
| Comments | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





### Create contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| First name | STRING | TEXT  |
| Last name | STRING | TEXT  |
| Company | STRING | SELECT  |
| Phone | STRING | TEXT  |
| Email | STRING | EMAIL  |
| Position | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





### Create task
Creates a new task

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Title | STRING | TEXT  |
| Against type | STRING | SELECT  |
| Against object | STRING | SELECT  |
| Start date | DATE | DATE  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





