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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| domain | Domain | STRING | TEXT | Your helpdesk domain name, e.g. https://{your_domain}.freshdesk.com/api/v2 | true |
| username | API key | STRING | TEXT |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Company | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(description), STRING\(note)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {NUMBER\(id), STRING\(name), STRING\(description), STRING\(note)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "__item" : {
      "name" : "",
      "description" : "",
      "note" : ""
    }
  },
  "type" : "freshdesk/v1/createCompany"
}
```


### Create Contact
Name: createContact

Creates a new contact

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(email), STRING\(phone), STRING\(mobile), STRING\(description), STRING\(job_title)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(description), STRING\(email), NUMBER\(id), STRING\(job_title)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
      "name" : "",
      "email" : "",
      "phone" : "",
      "mobile" : "",
      "description" : "",
      "job_title" : ""
    }
  },
  "type" : "freshdesk/v1/createContact"
}
```


### Create Ticket
Name: createTicket

Creates a new ticket

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Ticket | OBJECT <details> <summary> Properties </summary> {STRING\(subject), STRING\(email), STRING\(description), INTEGER\(priority), INTEGER\(status)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(subject), STRING\(email), STRING\(description), INTEGER\(priority), INTEGER\(status)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Ticket",
  "name" : "createTicket",
  "parameters" : {
    "__item" : {
      "subject" : "",
      "email" : "",
      "description" : "",
      "priority" : 1,
      "status" : 1
    }
  },
  "type" : "freshdesk/v1/createTicket"
}
```




