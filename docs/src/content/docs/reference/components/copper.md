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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| username | Email address | STRING | TEXT |  | true |
| key | Key | STRING | TEXT |  | true |





<hr />



## Actions


### Create Activity
Name: createActivity

Creates a new activity.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| activity_type | Activity Type ID | STRING | SELECT | Id of activity type for this activity. | true |
| details | Details | STRING | TEXT | Text body of this activity. | true |
| type | Parent Type | STRING <details> <summary> Options </summary> lead, person, company, opportunity </details> | SELECT | Parent type to associate this activity with. | true |
| id | Parent ID | STRING <details> <summary> Depends On </summary> type </details> | SELECT | ID of the parent this activity will be associated with. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| type | OBJECT <details> <summary> Properties </summary> {STRING\(category), STRING\(id)} </details> | OBJECT_BUILDER |
| details | STRING | TEXT |
| parent | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Name | STRING | TEXT | The name of the company. | true |
| assignee_id | Assignee ID | STRING | SELECT | ID of the user that will be the owner of the company. | false |
| email_domain | Email Domain | STRING | TEXT | The domain to which email addresses for the company belong. | false |
| contact_type_id | Contact Type ID | STRING | SELECT | ID of the Contact type for the company. | false |
| details | Details | STRING | TEXT | Description of the company. | false |
| phone_numbers | Phone Numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> | ARRAY_BUILDER | Phone numbers belonging to the company. | false |
| socials | Socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER | Social profiles belonging to the company. | false |
| websites | Websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER | Websites belonging to the company. | false |
| address | Address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> | OBJECT_BUILDER | Company's street, city, state, postal code, and country. | false |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Tags associated with the company | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| name | STRING | TEXT |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> | OBJECT_BUILDER |
| assignee_id | STRING | TEXT |
| contact_type_id | STRING | TEXT |
| details | STRING | TEXT |
| email_domain | STRING | TEXT |
| phone_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> | ARRAY_BUILDER |
| socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER |
| tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Name | STRING | TEXT | The first and last name of the person. | true |
| emails | Emails | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(category)}\($Email)] </details> | ARRAY_BUILDER | Email addresses belonging to the person. | false |
| assignee_id | Assignee ID | STRING | SELECT | User ID that will be the owner of the person. | false |
| title | Title | STRING | TEXT | The professional title of the person. | false |
| company_id | Company ID | STRING | SELECT | ID of the primary company with which the person is associated. | false |
| contact_type_id | Contact Type ID | STRING | SELECT | The unique identifier of the contact type of the person. | false |
| details | Details | STRING | TEXT | Description of the person. | false |
| phone_numbers | Phone Numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> | ARRAY_BUILDER | Phone numbers belonging to the person. | false |
| socials | Socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER | Social profiles belonging to the person. | false |
| websites | Websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER | Websites belonging to the person. | false |
| address | Address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> | OBJECT_BUILDER | Person's street, city, state, postal code, and country. | false |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | MULTI_SELECT | Tags associated with the person. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| name | STRING | TEXT |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} </details> | OBJECT_BUILDER |
| assignee_id | STRING | TEXT |
| company_id | STRING | TEXT |
| contact_type_id | STRING | TEXT |
| details | STRING | TEXT |
| emails | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(category)}] </details> | ARRAY_BUILDER |
| phone_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(number), STRING\(category)}] </details> | ARRAY_BUILDER |
| socials | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER |
| tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| title | STRING | TEXT |
| websites | ARRAY <details> <summary> Items </summary> [{STRING\(url), STRING\(category)}] </details> | ARRAY_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Name | STRING | TEXT | The name of the task. | true |
| assignee_id | Assignee ID | STRING | SELECT | ID of the user to assign the task to. | false |
| due_date | Due Date | DATE | DATE | The due date of the task. | false |
| reminder_date | Reminder Date | DATE | DATE | The reminder date of the task. | false |
| details | Description | STRING | TEXT | Description of the task. | false |
| priority | Priority | STRING <details> <summary> Options </summary> None, Low, Medium, High </details> | SELECT | The priority of the task. | true |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |  | false |
| status | Status | STRING <details> <summary> Options </summary> Open, Completed </details> | SELECT | The status of the task. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| name | STRING | TEXT |
| related_resource | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(type)} </details> | OBJECT_BUILDER |
| assignee_id | STRING | TEXT |
| due_date | STRING | TEXT |
| reminder_date | STRING | TEXT |
| completed_date | STRING | TEXT |
| priority | STRING | TEXT |
| status | STRING | TEXT |
| details | STRING | TEXT |
| tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| custom_fields | ARRAY <details> <summary> Items </summary> [] </details> | ARRAY_BUILDER |
| date_created | STRING | TEXT |
| date_modified | STRING | TEXT |




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




