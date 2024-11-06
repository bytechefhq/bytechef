---
title: "VTiger"
description: "VTiger is a comprehensive customer relationship management (CRM) platform that offers sales, marketing, and support solutions to streamline business."
---
## Reference
<hr />

VTiger is a comprehensive customer relationship management (CRM) platform that offers sales, marketing, and support solutions to streamline business.


Categories: [crm]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  Enter your username/email.  |
| Access Key | STRING | TEXT  |  |
| VTiger Instance URL | STRING | TEXT  |  For the instance URL, add the url without the endpoint.  |





<hr />





## Actions


### Create Contact
Creates a new contact.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First Name | STRING | TEXT  |  First name of the contact.  |
| Last Name | STRING | TEXT  |  Last name of the contact.  |
| Email | STRING | TEXT  |  Email address of the contact.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(assigned_user_id), STRING\(id)} | OBJECT_BUILDER  |






### Create Product
Creates a new product for your CRM.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Product Name | STRING | TEXT  |  Name of the product.  |
| Product Type | STRING | SELECT  |  Type of the product.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(productname), STRING\(product_type), STRING\(assigned_user_id), STRING\(id)} | OBJECT_BUILDER  |






### Get Me
Get more information about yourself.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(user_name), STRING\(user_type), STRING\(email), STRING\(phone_home), STRING\(phone_work), STRING\(phone_mobile), STRING\(userlable), STRING\(address_street), STRING\(address_city), STRING\(address_state), STRING\(address_country), STRING\(roleid), STRING\(language), STRING\(is_admin), STRING\(is_owner), STRING\(status)} | OBJECT_BUILDER  |






