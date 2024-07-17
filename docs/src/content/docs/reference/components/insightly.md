---
title: "Insightly"
description: "Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform."
---
## Reference
<hr />

Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform.


Categories: [CRM]


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


### Create contact
Creates new Contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {STRING(FIRST_NAME), STRING(LAST_NAME), STRING(EMAIL_ADDRESS), STRING(PHONE), STRING(TITLE)} | OBJECT_BUILDER  |  |


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





### Create organization
Creates new Organization

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Organization | {STRING(ORGANISATION_NAME), STRING(PHONE), STRING(WEBSITE)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Create task
Creates new Task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | {STRING(TITLE), STRING(STATUS)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |





