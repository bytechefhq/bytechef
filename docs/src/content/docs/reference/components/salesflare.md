---
title: "Salesflare"
description: "Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently."
---
## Reference
<hr />

Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently.


Categories: [CRM]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create account
Creates new account

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Account | {STRING(name), STRING(website), STRING(description), STRING(email), STRING(phone_number), [STRING](social_profiles)} | OBJECT_BUILDER  |  |




### Create contacts
Creates new contacts

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contacts | [{STRING(email), STRING(firstname), STRING(lastname), STRING(phone_number), STRING(mobile_phone_number), STRING(home_phone_number), STRING(fax_number), [STRING](social_profiles)}] | ARRAY_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{INTEGER(id)}] | ARRAY_BUILDER  |





### Create tasks
Creates new tasks

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Tasks | [{STRING(description), DATE(reminder_date)}] | ARRAY_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{INTEGER(id)}] | ARRAY_BUILDER  |





