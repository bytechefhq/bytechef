---
title: "Salesflare"
description: "Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently."
---

Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently.


Categories: crm


Type: salesflare/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Account
Creates new account.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Account | {STRING\(name), STRING\(website), STRING\(description), STRING\(email), STRING\(phone_number), [STRING]\(social_profiles)} | OBJECT_BUILDER  |  | true  |




### Create Contacts
Creates new contacts.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __items | Contacts | [{STRING\(email), STRING\(firstname), STRING\(lastname), STRING\(phone_number), STRING\(mobile_phone_number), STRING\(home_phone_number), STRING\(fax_number), [STRING]\(social_profiles)}] | ARRAY_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | [{INTEGER\(id)}] | ARRAY_BUILDER  |






### Create Tasks
Creates new tasks.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __items | Tasks | [{STRING\(description), DATE\(reminder_date)}] | ARRAY_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | [{INTEGER\(id)}] | ARRAY_BUILDER  |








## Triggers



<hr />

