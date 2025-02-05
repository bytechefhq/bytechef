---
title: "ActiveCampaign"
description: "ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools."
---

ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools.


Categories: crm, marketing-automation


Type: active-campaign/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Account name | STRING | TEXT  |  Your account name, e.g. https://{youraccountname}.api-us1.com  |  true  |
| key | Key | STRING | TEXT  |  | true  |
| value | API Key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Account
Creates a new account.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Account | {{STRING\(name), STRING\(accountUrl)}\(account)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(name), STRING\(accountUrl)}\(account)} | OBJECT_BUILDER  |






### Create Contact
Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(phone)}\(contact)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(phone)}\(contact)} | OBJECT_BUILDER  |






### Create Task
Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Task | {{STRING\(title), INTEGER\(relid), DATE\(duedate), INTEGER\(dealTasktype)}\(dealTask)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(id), STRING\(title), INTEGER\(relid), DATE\(duedate), INTEGER\(dealTasktype)}\(dealTask)} | OBJECT_BUILDER  |








## Triggers



<hr />

