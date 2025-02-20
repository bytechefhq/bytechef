---
title: "Zendesk Sell"
description: "Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently."
---

Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently.


Categories: crm


Type: zendeskSell/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | Token | STRING | TEXT |  | true |





<hr />



## Actions


### Create Contact
Name: createContact

Creates new contact. A contact may represent a single individual or an organization.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| is_organization | Is Contact Represent an Organization? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Is contact represent an organization or a single individual? | true |
| name | Name | STRING | TEXT | The name of the organisation. | false |
| first_name | First Name | STRING | TEXT | The first name of the person. | false |
| last_name | Last Name | STRING | TEXT | The last name of the person. | true |
| title | Title | STRING | TEXT |  | false |
| website | Website | STRING | TEXT |  | false |
| email | Email | STRING | EMAIL |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| data | OBJECT <details> <summary> Properties </summary> {STRING\(id), BOOLEAN\(is_organization), STRING\(title), STRING\(website), STRING\(email)} </details> | OBJECT_BUILDER |
| meta | OBJECT <details> <summary> Properties </summary> {INTEGER\(version), STRING\(type)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "is_organization" : false,
    "name" : "",
    "first_name" : "",
    "last_name" : "",
    "title" : "",
    "website" : "",
    "email" : ""
  },
  "type" : "zendeskSell/v1/createContact"
}
```


### Create Task
Name: createTask

Creates new task.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| content | Task Name | STRING | TEXT |  | true |
| due_date | Due Date | DATE | DATE |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| data | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(content), DATE\(due_date)} </details> | OBJECT_BUILDER |
| meta | OBJECT <details> <summary> Properties </summary> {STRING\(type)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "content" : "",
    "due_date" : "2021-01-01"
  },
  "type" : "zendeskSell/v1/createTask"
}
```




