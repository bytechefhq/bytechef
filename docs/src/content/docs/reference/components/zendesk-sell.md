---
title: "Zendesk Sell"
description: "Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently."
---
## Reference
<hr />

Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently.


Categories: [CRM]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |





<hr />





## Actions


### Create contact
Creates new contact. A contact may represent a single individual or an organization.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Is contact represent an organization? | BOOLEAN | SELECT  |
| Name | STRING | TEXT  |
| First name | STRING | TEXT  |
| Last name | STRING | TEXT  |
| Title | STRING | TEXT  |
| Website | STRING | TEXT  |
| Email | STRING | EMAIL  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





### Create task
Creates new Task

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Task name | STRING | TEXT  |
| Due Date | DATE | DATE  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





