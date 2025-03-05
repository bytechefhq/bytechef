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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| domain | Domain | STRING | Your helpdesk domain name, e.g. https://{your_domain}.freshdesk.com/api/v2 | true |
| username | API key | STRING |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | Name of the company | true |
| description | Description | STRING | Description of the company | false |
| note | Note | STRING | Any specific note about the company | false |

#### Example JSON Structure
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "name" : "",
    "description" : "",
    "note" : ""
  },
  "type" : "freshdesk/v1/createCompany"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | NUMBER |  |
| name | STRING |  |
| description | STRING |  |
| note | STRING |  |




#### Output Example
```json
{
  "id" : 0.0,
  "name" : "",
  "description" : "",
  "note" : ""
}
```


### Create Contact
Name: createContact

Creates a new contact

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | Full name of the contact | true |
| email | Email | STRING | Primary email address of the contact. | true |
| phone | Work Phone | STRING | Telephone number of the contact | false |
| mobile | Mobile | STRING | Mobile number of the contact | false |
| description | Description | STRING | A small description of the contact | false |
| job_title | Job Title | STRING | Job title of the contact | false |

#### Example JSON Structure
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "name" : "",
    "email" : "",
    "phone" : "",
    "mobile" : "",
    "description" : "",
    "job_title" : ""
  },
  "type" : "freshdesk/v1/createContact"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| description | STRING |  |
| email | STRING |  |
| id | NUMBER |  |
| job_title | STRING |  |




#### Output Example
```json
{
  "description" : "",
  "email" : "",
  "id" : 0.0,
  "job_title" : ""
}
```


### Create Ticket
Name: createTicket

Creates a new ticket

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| subject | Subject | STRING | Subject of the ticket. | true |
| email | Email | STRING | Email address of the requester. If no contact exists with this email address in Freshdesk, it will be added as a new contact. | true |
| description | Description | STRING | HTML content of the ticket. | true |
| priority | Priority | INTEGER <details> <summary> Options </summary> 1, 2, 3, 4 </details> | Priority of the ticket. | false |
| status | Status | INTEGER <details> <summary> Options </summary> 2, 3, 4, 5 </details> | Status of the ticket. | false |

#### Example JSON Structure
```json
{
  "label" : "Create Ticket",
  "name" : "createTicket",
  "parameters" : {
    "subject" : "",
    "email" : "",
    "description" : "",
    "priority" : 1,
    "status" : 1
  },
  "type" : "freshdesk/v1/createTicket"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| subject | STRING |  |
| email | STRING |  |
| description | STRING |  |
| priority | INTEGER | Priority of the ticket. |
| status | INTEGER |  |




#### Output Example
```json
{
  "subject" : "",
  "email" : "",
  "description" : "",
  "priority" : 1,
  "status" : 1
}
```




