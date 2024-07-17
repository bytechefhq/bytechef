---
title: "Pipeliner"
description: "Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting."
---
## Reference
<hr />

Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting.


Categories: [CRM]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Space Id | STRING | TEXT  |  Your Space ID  |
| Server URL | STRING | SELECT  |  |
| Username | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create account
Creates new account

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Account | {STRING(owner_id), STRING(name)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {STRING(id), STRING(owner_id), STRING(name)} | OBJECT_BUILDER  |





### Create contact
Creates new Contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {STRING(owner_id), STRING(first_name), STRING(last_name)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {STRING(id), STRING(owner_id), STRING(first_name), STRING(last_name)} | OBJECT_BUILDER  |





### Create task
Creates new Task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | {STRING(subject), STRING(activity_type_id), STRING(unit_id), STRING(owner_id)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {STRING(id), STRING(subject), STRING(activity_type_id), STRING(unit_id), STRING(owner_id)} | OBJECT_BUILDER  |





