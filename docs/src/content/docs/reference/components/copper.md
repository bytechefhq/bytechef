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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Email address | STRING | TEXT  |  | true  |
| key | Key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Activity
Creates a new activity.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| activity_type | Activity Type ID | STRING | SELECT  |  Id of activity type for this activity.  |  true  |
| details | Details | STRING | TEXT  |  Text body of this activity.  |  true  |
| type | Parent Type | STRING | SELECT  |  Parent type to associate this activity with.  |  true  |
| id | Parent ID | STRING | SELECT  |  ID of the parent this activity will be associated with.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| type | {STRING\(category), STRING\(id)} | OBJECT_BUILDER  |
| details | STRING | TEXT  |
| parent | {STRING\(type), STRING\(id)} | OBJECT_BUILDER  |






### Create Company
Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| name | Name | STRING | TEXT  |  The name of the company.  |  true  |
| assignee_id | Assignee ID | STRING | SELECT  |  ID of the user that will be the owner of the company.  |  false  |
| email_domain | Email Domain | STRING | TEXT  |  The domain to which email addresses for the company belong.  |  false  |
| contact_type_id | Contact Type ID | STRING | SELECT  |  ID of the Contact type for the company.  |  false  |
| details | Details | STRING | TEXT  |  Description of the company.  |  false  |
| phone_numbers | Phone Numbers | [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |  Phone numbers belonging to the company.  |  false  |
| socials | Socials | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Social profiles belonging to the company.  |  false  |
| websites | Websites | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Websites belonging to the company.  |  false  |
| address | Address | {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |  Company's street, city, state, postal code, and country.  |  false  |
| tags | Tags | [STRING] | ARRAY_BUILDER  |  Tags associated with the company  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |
| address | {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |
| assignee_id | STRING | TEXT  |
| contact_type_id | STRING | TEXT  |
| details | STRING | TEXT  |
| email_domain | STRING | TEXT  |
| phone_numbers | [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |
| socials | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |
| tags | [STRING] | ARRAY_BUILDER  |
| websites | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |






### Create Person
Creates a new person.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| name | Name | STRING | TEXT  |  The first and last name of the person.  |  true  |
| emails | Emails | [{STRING\(email), STRING\(category)}\($Email)] | ARRAY_BUILDER  |  Email addresses belonging to the person.  |  false  |
| assignee_id | Assignee ID | STRING | SELECT  |  User ID that will be the owner of the person.  |  false  |
| title | Title | STRING | TEXT  |  The professional title of the person.  |  false  |
| company_id | Company ID | STRING | SELECT  |  ID of the primary company with which the person is associated.  |  false  |
| contact_type_id | Contact Type ID | STRING | SELECT  |  The unique identifier of the contact type of the person.  |  false  |
| details | Details | STRING | TEXT  |  Description of the person.  |  false  |
| phone_numbers | Phone Numbers | [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |  Phone numbers belonging to the person.  |  false  |
| socials | Socials | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Social profiles belonging to the person.  |  false  |
| websites | Websites | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Websites belonging to the person.  |  false  |
| address | Address | {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |  Person's street, city, state, postal code, and country.  |  false  |
| tags | Tags | [STRING] | ARRAY_BUILDER  |  Tags associated with the person.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |
| address | {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |
| assignee_id | STRING | TEXT  |
| company_id | STRING | TEXT  |
| contact_type_id | STRING | TEXT  |
| details | STRING | TEXT  |
| emails | [{STRING\(email), STRING\(category)}] | ARRAY_BUILDER  |
| phone_numbers | [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |
| socials | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |
| tags | [STRING] | ARRAY_BUILDER  |
| title | STRING | TEXT  |
| websites | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |






### Create Task
Creates a new task in Copper.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| name | Name | STRING | TEXT  |  The name of the task.  |  true  |
| assignee_id | Assignee ID | STRING | SELECT  |  ID of the user to assign the task to.  |  false  |
| due_date | Due Date | DATE | DATE  |  The due date of the task.  |  false  |
| reminder_date | Reminder Date | DATE | DATE  |  The reminder date of the task.  |  false  |
| details | Description | STRING | TEXT  |  Description of the task.  |  false  |
| priority | Priority | STRING | SELECT  |  The priority of the task.  |  true  |
| tags | Tags | [STRING] | ARRAY_BUILDER  |  | false  |
| status | Status | STRING | SELECT  |  The status of the task.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |
| related_resource | {STRING\(id), STRING\(type)} | OBJECT_BUILDER  |
| assignee_id | STRING | TEXT  |
| due_date | STRING | TEXT  |
| reminder_date | STRING | TEXT  |
| completed_date | STRING | TEXT  |
| priority | STRING | TEXT  |
| status | STRING | TEXT  |
| details | STRING | TEXT  |
| tags | [STRING] | ARRAY_BUILDER  |
| custom_fields | [] | ARRAY_BUILDER  |
| date_created | STRING | TEXT  |
| date_modified | STRING | TEXT  |








