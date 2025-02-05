---
title: "Pipeliner"
description: "Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting."
---

Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting.


Categories: crm


Type: pipeliner/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| spaceId | Space Id | STRING | TEXT  |  Your Space ID  |  true  |
| serverUrl | Server URL | STRING | SELECT  |  | true  |
| username | Username | STRING | TEXT  |  | true  |
| password | Password | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Account
Creates new account.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Account | {STRING\(owner_id), STRING\(name)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| success | BOOLEAN | SELECT  |
| data | {STRING\(id), STRING\(owner_id), STRING\(name)} | OBJECT_BUILDER  |






### Create Contact
Creates new Contact

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {STRING\(owner_id), STRING\(first_name), STRING\(last_name)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| success | BOOLEAN | SELECT  |
| data | {STRING\(id), STRING\(owner_id), STRING\(first_name), STRING\(last_name)} | OBJECT_BUILDER  |






### Create Task
Creates new Task

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Task | {STRING\(subject), STRING\(activity_type_id), STRING\(unit_id), STRING\(owner_id)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| success | BOOLEAN | SELECT  |
| data | {STRING\(id), STRING\(subject), STRING\(activity_type_id), STRING\(unit_id), STRING\(owner_id)} | OBJECT_BUILDER  |








## Triggers



<hr />

