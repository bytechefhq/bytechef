---
title: "Freshdesk"
description: "Freshdesk is a cloud-based customer support software that helps businesses manage customer queries and tickets efficiently."
---

Freshdesk is a cloud-based customer support software that helps businesses manage customer queries and tickets efficiently.


Categories: customer-support


Type: freshdesk/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| domain | Domain | STRING | TEXT  |  Your helpdesk domain name, e.g. https://{your_domain}.freshdesk.com/api/v2  |  true  |
| username | API key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Company
Creates a new company

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Company | {STRING\(name), STRING\(description), STRING\(note)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {NUMBER\(id), STRING\(name), STRING\(description), STRING\(note)} | OBJECT_BUILDER  |






### Create Contact
Creates a new contact

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {STRING\(name), STRING\(email), STRING\(phone), STRING\(mobile), STRING\(description), STRING\(job_title)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(description), STRING\(email), NUMBER\(id), STRING\(job_title)} | OBJECT_BUILDER  |






### Create Ticket
Creates a new ticket

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Ticket | {STRING\(subject), STRING\(email), STRING\(description), INTEGER\(priority), INTEGER\(status)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(subject), STRING\(email), STRING\(description), INTEGER\(priority), INTEGER\(status)} | OBJECT_BUILDER  |








## Triggers



<hr />

