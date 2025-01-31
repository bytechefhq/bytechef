---
title: "Copper"
description: "Copper is a customer relationship management (CRM) software designed to streamline and optimize sales processes, providing tools for managing contact, leads, opportunities, and communications in one centralized platform."
---
## Reference
<hr />

Copper is a customer relationship management (CRM) software designed to streamline and optimize sales processes, providing tools for managing contact, leads, opportunities, and communications in one centralized platform.


Categories: [crm]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Email address | STRING | TEXT  |  |
| Key | STRING | TEXT  |  |





<hr />





## Actions


### Create Activity
Creates a new activity.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Activity Type ID | STRING | SELECT  |  Id of activity type for this activity.  |
| Details | STRING | TEXT  |  Text body of this activity.  |
| Parent Type | STRING | SELECT  |  Parent type to associate this activity with.  |
| Parent ID | STRING | SELECT  |  ID of the parent this activity will be associated with.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| {STRING\(category), STRING\(id)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| {STRING\(type), STRING\(id)} | OBJECT_BUILDER  |






### Create Company
Creates a new company.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name of the company.  |
| Assignee ID | STRING | SELECT  |  ID of the user that will be the owner of the company.  |
| Email Domain | STRING | TEXT  |  The domain to which email addresses for the company belong.  |
| Contact Type ID | STRING | SELECT  |  ID of the Contact type for the company.  |
| Details | STRING | TEXT  |  Description of the company.  |
| Phone Numbers | [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |  Phone numbers belonging to the company.  |
| Socials | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Social profiles belonging to the company.  |
| Websites | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Websites belonging to the company.  |
| Address | {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |  Company's street, city, state, postal code, and country.  |
| Tags | [STRING] | ARRAY_BUILDER  |  Tags associated with the company  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |
| [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |
| [STRING] | ARRAY_BUILDER  |
| [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |






### Create Person
Creates a new person.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The first and last name of the person.  |
| Emails | [{STRING\(email), STRING\(category)}\($Email)] | ARRAY_BUILDER  |  Email addresses belonging to the person.  |
| Assignee ID | STRING | SELECT  |  User ID that will be the owner of the person.  |
| Title | STRING | TEXT  |  The professional title of the person.  |
| Company ID | STRING | SELECT  |  ID of the primary company with which the person is associated.  |
| Contact Type ID | STRING | SELECT  |  The unique identifier of the contact type of the person.  |
| Details | STRING | TEXT  |  Description of the person.  |
| Phone Numbers | [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |  Phone numbers belonging to the person.  |
| Socials | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Social profiles belonging to the person.  |
| Websites | [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |  Websites belonging to the person.  |
| Address | {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |  Person's street, city, state, postal code, and country.  |
| Tags | [STRING] | ARRAY_BUILDER  |  Tags associated with the person.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(street), STRING\(city), STRING\(state), STRING\(postal_code), STRING\(country)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING\(email), STRING\(category)}] | ARRAY_BUILDER  |
| [{STRING\(number), STRING\(category)}] | ARRAY_BUILDER  |
| [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| [{STRING\(url), STRING\(category)}] | ARRAY_BUILDER  |






### Create Task
Creates a new task in Copper.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name of the task.  |
| Assignee ID | STRING | SELECT  |  ID of the user to assign the task to.  |
| Due Date | DATE | DATE  |  The due date of the task.  |
| Reminder Date | DATE | DATE  |  The reminder date of the task.  |
| Description | STRING | TEXT  |  Description of the task.  |
| Priority | STRING | SELECT  |  The priority of the task.  |
| Tags | [STRING] | ARRAY_BUILDER  |  |
| Status | STRING | SELECT  |  The status of the task.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(id), STRING\(type)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |
| [] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |






