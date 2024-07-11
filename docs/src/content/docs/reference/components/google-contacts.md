---
title: "Google Contacts"
description: "Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms."
---
## Reference
<hr />

Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms.

Categories: [CRM]

Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />





## Actions


### Create contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| First name | STRING | TEXT  |
| Middle name | STRING | TEXT  |
| Last name | STRING | TEXT  |
| Job title | STRING | TEXT  |
| Company | STRING | TEXT  |
| Email | STRING | EMAIL  |
| Phone number | STRING | PHONE  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |





### Create groups
Creates a new group

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Group name | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |





