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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING | Enter your username/email. | true |
| password | Access Key | STRING |  | true |
| instance_url | VTiger Instance URL | STRING | For the instance URL, add the url without the endpoint. | true |





<hr />



## Actions


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| firstname | First Name | STRING | First name of the contact. | true |
| lastname | Last Name | STRING | Last name of the contact. | true |
| email | Email | STRING | Email address of the contact. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| result | OBJECT <details> <summary> Properties </summary> {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(assigned_user_id), STRING\(id)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "firstname" : "",
    "lastname" : "",
    "email" : ""
  },
  "type" : "vtiger/v1/createContact"
}
```


### Create Product
Name: createProduct

Creates a new product for your CRM.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| productname | Product Name | STRING | Name of the product. | true |
| product_type | Product Type | STRING <details> <summary> Options </summary> Solo, Fixed Bundle </details> | Type of the product. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| results | OBJECT <details> <summary> Properties </summary> {STRING\(productname), STRING\(product_type), STRING\(assigned_user_id), STRING\(id)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create Product",
  "name" : "createProduct",
  "parameters" : {
    "productname" : "",
    "product_type" : ""
  },
  "type" : "vtiger/v1/createProduct"
}
```


### Get Me
Name: getMe

Get more information about yourself.


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| result | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(user_name), STRING\(user_type), STRING\(email), STRING\(phone_home), STRING\(phone_work), STRING\(phone_mobile), STRING\(userlable), STRING\(address_street), STRING\(address_city), STRING\(address_state), STRING\(address_country), STRING\(roleid), STRING\(language), STRING\(is_admin), STRING\(is_owner), STRING\(status)} </details> |  |




#### JSON Example
```json
{
  "label" : "Get Me",
  "name" : "getMe",
  "type" : "vtiger/v1/getMe"
}
```




