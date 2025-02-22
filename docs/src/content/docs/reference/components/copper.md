---
title: "Copper"
description: "Copper is a customer relationship management (CRM) software designed to streamline and optimize sales processes, providing tools for managing contact, leads, opportunities, and communications in one centralized platform."
---

Copper is a customer relationship management (CRM) software designed to streamline and optimize sales processes, providing tools for managing contact, leads, opportunities, and communications in one centralized platform.


Categories: crm


Type: copper/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Email address | STRING |  | true |
| key | Key | STRING |  | true |





<hr />



## Actions


### Create Activity
Name: createActivity

Creates a new activity.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| activity_type | Activity Type ID | STRING | Id of activity type for this activity. | true |
| details | Details | STRING | Text body of this activity. | true |
| type | Parent Type | STRING <details> <summary> Options </summary> lead, person, company, opportunity </details> | Parent type to associate this activity with. | true |
| id | Parent ID | STRING <details> <summary> Depends On </summary> type </details> | ID of the parent this activity will be associated with. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| type | OBJECT <details> <summary> Properties </summary> {STRING\(category), STRING\(id)} </details> |
| details | STRING |
| parent | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id)} </details> |




#### JSON Example
```json
{
  "label" : "Create Activity",
  "name" : "createActivity",
  "parameters" : {
    "activity_type" : "",
    "details" : "",
    "type" : "",
    "id" : ""
  },
  "type" : "copper/v1/createActivity"
}
```


### Create Company
Name: createCompany

Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The name of the company. | true |
| assignee_id | Assignee ID | STRING | ID of the user that will be the owner of the company. | false |
| email_domain | Email Domain | STRING | The domain to which email addresses for the company belong. | false |
| contact_type_id | Contact Type ID | STRING | ID of the Contact type for the company. | false |
| details | Details | STRING | Description of the company. | false |
| phone_numbers | Phone Numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> | Phone numbers belonging to the company. | false |
| socials | Socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | Social profiles belonging to the company. | false |
| websites | Websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | Websites belonging to the company. | false |
| address | Address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> | Company's street, city, state, postal code, and country. | false |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | Tags associated with the company | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| name | STRING |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> |
| assignee_id | STRING |
| contact_type_id | STRING |
| details | STRING |
| email_domain | STRING |
| phone_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> |
| socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> |
| tags | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> |




#### JSON Example
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "name" : "",
    "assignee_id" : "",
    "email_domain" : "",
    "contact_type_id" : "",
    "details" : "",
    "phone_numbers" : [ {
      "number" : "",
      "category" : ""
    } ],
    "socials" : [ {
      "url" : "",
      "category" : ""
    } ],
    "websites" : [ {
      "url" : "",
      "category" : ""
    } ],
    "address" : {
      "street" : "",
      "city" : "",
      "state" : "",
      "postal_code" : "",
      "country" : ""
    },
    "tags" : [ "" ]
  },
  "type" : "copper/v1/createCompany"
}
```


### Create Person
Name: createPerson

Creates a new person.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The first and last name of the person. | true |
| emails | Emails | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(category)}\($Email)] </details> | Email addresses belonging to the person. | false |
| assignee_id | Assignee ID | STRING | User ID that will be the owner of the person. | false |
| title | Title | STRING | The professional title of the person. | false |
| company_id | Company ID | STRING | ID of the primary company with which the person is associated. | false |
| contact_type_id | Contact Type ID | STRING | The unique identifier of the contact type of the person. | false |
| details | Details | STRING | Description of the person. | false |
| phone_numbers | Phone Numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> | Phone numbers belonging to the person. | false |
| socials | Socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | Social profiles belonging to the person. | false |
| websites | Websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | Websites belonging to the person. | false |
| address | Address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> | Person's street, city, state, postal code, and country. | false |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | Tags associated with the person. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| name | STRING |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> |
| assignee_id | STRING |
| company_id | STRING |
| contact_type_id | STRING |
| details | STRING |
| emails | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(category)}] </details> |
| phone_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> |
| socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> |
| tags | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| title | STRING |
| websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> |




#### JSON Example
```json
{
  "label" : "Create Person",
  "name" : "createPerson",
  "parameters" : {
    "name" : "",
    "emails" : [ {
      "email" : "",
      "category" : ""
    } ],
    "assignee_id" : "",
    "title" : "",
    "company_id" : "",
    "contact_type_id" : "",
    "details" : "",
    "phone_numbers" : [ {
      "number" : "",
      "category" : ""
    } ],
    "socials" : [ {
      "url" : "",
      "category" : ""
    } ],
    "websites" : [ {
      "url" : "",
      "category" : ""
    } ],
    "address" : {
      "street" : "",
      "city" : "",
      "state" : "",
      "postal_code" : "",
      "country" : ""
    },
    "tags" : [ "" ]
  },
  "type" : "copper/v1/createPerson"
}
```


### Create Task
Name: createTask

Creates a new task in Copper.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The name of the task. | true |
| assignee_id | Assignee ID | STRING | ID of the user to assign the task to. | false |
| due_date | Due Date | DATE | The due date of the task. | false |
| reminder_date | Reminder Date | DATE | The reminder date of the task. | false |
| details | Description | STRING | Description of the task. | false |
| priority | Priority | STRING <details> <summary> Options </summary> None, Low, Medium, High </details> | The priority of the task. | true |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> |  | false |
| status | Status | STRING <details> <summary> Options </summary> Open, Completed </details> | The status of the task. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| name | STRING |
| related_resource | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(type)} </details> |
| assignee_id | STRING |
| due_date | STRING |
| reminder_date | STRING |
| completed_date | STRING |
| priority | STRING |
| status | STRING |
| details | STRING |
| tags | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| custom_fields | ARRAY <details> <summary> Items </summary> [] </details> |
| date_created | STRING |
| date_modified | STRING |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "name" : "",
    "assignee_id" : "",
    "due_date" : "2021-01-01",
    "reminder_date" : "2021-01-01",
    "details" : "",
    "priority" : "",
    "tags" : [ "" ],
    "status" : ""
  },
  "type" : "copper/v1/createTask"
}
```




