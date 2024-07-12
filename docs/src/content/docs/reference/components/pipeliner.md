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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Space Id | STRING | TEXT  |
| Server URL | STRING | SELECT  |
| Username | STRING | TEXT  |
| Password | STRING | TEXT  |





<hr />



## Triggers



<hr />



## Actions


### Create account
Creates new account

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Account | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| OBJECT | OBJECT_BUILDER  |





### Create contact
Creates new Contact

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Contact | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| OBJECT | OBJECT_BUILDER  |





### Create task
Creates new Task

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Task | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| OBJECT | OBJECT_BUILDER  |





