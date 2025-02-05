---
title: "VTiger"
description: "VTiger is a comprehensive customer relationship management (CRM) platform that offers sales, marketing, and support solutions to streamline business."
---

VTiger is a comprehensive customer relationship management (CRM) platform that offers sales, marketing, and support solutions to streamline business.


Categories: crm


Type: vtiger/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  Enter your username/email.  |  true  |
| password | Access Key | STRING | TEXT  |  | true  |
| instance_url | VTiger Instance URL | STRING | TEXT  |  For the instance URL, add the url without the endpoint.  |  true  |





<hr />



## Actions


### Create Contact
Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| firstname | First Name | STRING | TEXT  |  First name of the contact.  |  true  |
| lastname | Last Name | STRING | TEXT  |  Last name of the contact.  |  true  |
| email | Email | STRING | TEXT  |  Email address of the contact.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| result | {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(assigned_user_id), STRING\(id)} | OBJECT_BUILDER  |






### Create Product
Creates a new product for your CRM.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| productname | Product Name | STRING | TEXT  |  Name of the product.  |  true  |
| product_type | Product Type | STRING | SELECT  |  Type of the product.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| results | {STRING\(productname), STRING\(product_type), STRING\(assigned_user_id), STRING\(id)} | OBJECT_BUILDER  |






### Get Me
Get more information about yourself.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| result | {STRING\(id), STRING\(user_name), STRING\(user_type), STRING\(email), STRING\(phone_home), STRING\(phone_work), STRING\(phone_mobile), STRING\(userlable), STRING\(address_street), STRING\(address_city), STRING\(address_state), STRING\(address_country), STRING\(roleid), STRING\(language), STRING\(is_admin), STRING\(is_owner), STRING\(status)} | OBJECT_BUILDER  |








