---
title: "ActiveCampaign"
description: "ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools."
---
## Reference
<hr />

ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools.


Categories: [CRM, MARKETING_AUTOMATION]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Account name | STRING | TEXT  |  Your account name, e.g. https://{youraccountname}.api-us1.com  |
| Key | STRING | TEXT  |  |
| API Key | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create account
Creates a new account

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Account | {{STRING(name), STRING(accountUrl)}(account)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING(name), STRING(accountUrl)}(account)} | OBJECT_BUILDER  |





### Create contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {{STRING(email), STRING(firstName), STRING(lastName), STRING(phone)}(contact)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING(email), STRING(firstName), STRING(lastName), STRING(phone)}(contact)} | OBJECT_BUILDER  |





### Creates a task
Creates a new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | {{STRING(title), INTEGER(relid), DATE(duedate), INTEGER(dealTasktype)}(dealTask)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING(id), STRING(title), INTEGER(relid), DATE(duedate), INTEGER(dealTasktype)}(dealTask)} | OBJECT_BUILDER  |





