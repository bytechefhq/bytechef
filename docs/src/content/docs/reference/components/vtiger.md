---
title: "VTiger"
description: "CRM software for sales, marketing, and support teams"
---
## Reference
<hr />

CRM software for sales, marketing, and support teams


Categories: [CRM]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| VTiger Username of email | STRING | TEXT  |  |
| VTiger Access Key | STRING | TEXT  |  |
| VTiger Instance URL | STRING | TEXT  |  For the instance URL, add the url without the endpoint. For example enter https://<instance>.od2.vtiger.com instead of https://<instance>.od2.vtiger.com/restapi/v1/vtiger/default  |





<hr />





## Actions


### Create a contact
Create a new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First Name | STRING | TEXT  |  First name of the contact  |
| Last Name | STRING | TEXT  |  Last name of the contact  |
| Contact email | STRING | TEXT  |  email for your new contact  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(firstname), STRING(lastname), STRING(email), STRING(phone)} | OBJECT_BUILDER  |





### Create a Product
Create a new Product for your CRM

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Product Name | STRING | TEXT  |  Name of the product  |
| Product Type | STRING | SELECT  |  Type of the product  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(productname), STRING(product_type), STRING(createdtime), STRING(source), STRING(assigned_user_id)} | OBJECT_BUILDER  |





### Get Me
Get more information about yourself

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(user_name), STRING(user_type), STRING(email), STRING(phone_home), STRING(phone_work), STRING(phone_mobile), STRING(userlable), STRING(address_street), STRING(address_city), STRING(address_state), STRING(address_country), STRING(roleid), STRING(language), STRING(is_admin), STRING(is_owner), STRING(status)} | OBJECT_BUILDER  |





