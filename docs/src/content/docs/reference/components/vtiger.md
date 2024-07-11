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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| VTiger Username of email | STRING | TEXT  |
| VTiger Access Key | STRING | TEXT  |
| VTiger Instance URL | STRING | TEXT  |





<hr />





## Actions


### Create a contact
Create a new contact

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| First Name | STRING | TEXT  |
| Last Name | STRING | TEXT  |
| Contact email | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Create a Product
Create a new Product for your CRM

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Product Name | STRING | TEXT  |
| Product Type | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Get Me
Get more information about yourself

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
null


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





