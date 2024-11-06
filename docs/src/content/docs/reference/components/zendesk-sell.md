---
title: "Zendesk Sell"
description: "Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently."
---
## Reference
<hr />

Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently.


Categories: [crm]


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





## Actions


### Create Contact
Creates new contact. A contact may represent a single individual or an organization.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Is Contact Represent an Organization? | BOOLEAN | SELECT  |  Is contact represent an organization or a single individual?  |
| Name | STRING | TEXT  |  The name of the organisation.  |
| First Name | STRING | TEXT  |  The first name of the person.  |
| Last Name | STRING | TEXT  |  The last name of the person.  |
| Title | STRING | TEXT  |  |
| Website | STRING | TEXT  |  |
| Email | STRING | EMAIL  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), BOOLEAN\(is_organization), STRING\(title), STRING\(website), STRING\(email)} | OBJECT_BUILDER  |
| {INTEGER\(version), STRING\(type)} | OBJECT_BUILDER  |






### Create Task
Creates new task.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task Name | STRING | TEXT  |  |
| Due Date | DATE | DATE  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {INTEGER\(id), STRING\(content), DATE\(due_date)} | OBJECT_BUILDER  |
| {STRING\(type)} | OBJECT_BUILDER  |






