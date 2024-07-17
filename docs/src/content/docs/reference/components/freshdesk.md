---
title: "Freshdesk"
description: "Freshdesk is a cloud-based customer support software that helps businesses manage customer queries and tickets efficiently."
---
## Reference
<hr />

Freshdesk is a cloud-based customer support software that helps businesses manage customer queries and tickets efficiently.


Categories: [CUSTOMER_SUPPORT]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Domain | STRING | TEXT  |  Your helpdesk domain name, e.g. https://{your_domain}.freshdesk.com/api/v2  |
| API key | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create company
Creates a new company

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company | {STRING(name), STRING(description), STRING(note)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {NUMBER(id), STRING(name), STRING(description), STRING(note)} | OBJECT_BUILDER  |





### Create contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {STRING(name), STRING(email), STRING(phone), STRING(mobile), STRING(description), STRING(job_title)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(description), STRING(email), NUMBER(id), STRING(job_title)} | OBJECT_BUILDER  |





### Create ticket
Creates a new ticket

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Ticket | {STRING(subject), STRING(email), STRING(description), INTEGER(priority), INTEGER(status)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(subject), STRING(email), STRING(description), INTEGER(priority), INTEGER(status)} | OBJECT_BUILDER  |





