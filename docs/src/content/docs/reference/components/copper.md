---
title: "Copper"
description: "Copper is a customer relationship management (CRM) software designed to streamline and optimize sales processes, providing tools for managing contact, leads, opportunities, and communications in one centralized platform."
---
## Reference
<hr />

Copper is a customer relationship management (CRM) software designed to streamline and optimize sales processes, providing tools for managing contact, leads, opportunities, and communications in one centralized platform.


Categories: [CRM]


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


### Create activity
Creates a new Activity

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Activity type | STRING | SELECT  |  The Activity Type of this Activity.  |
| Details | STRING | TEXT  |  Text body of this Activity.  |
| Parent type | STRING | SELECT  |  Parent type to associate this Activity with.  |
| Parent name | STRING | SELECT  |  Parent this Activity will be associated with.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| {STRING(category), STRING(id)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| {STRING(type), STRING(id)} | OBJECT_BUILDER  |





### Create company
Creates a new Company

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name of the Company.  |
| Assignee | STRING | SELECT  |  User that will be the owner of the Company.  |
| Email domain | STRING | TEXT  |  The domain to which email addresses for the Company belong.  |
| Contact type | STRING | SELECT  |  Contact Type of the Company.  |
| Details | STRING | TEXT  |  Description of the Company.  |
| Phone numbers | [{STRING(number), STRING(category)}] | ARRAY_BUILDER  |  Phone numbers belonging to the Company.  |
| Socials | [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |  Social profiles belonging to the Company.  |
| Websites | [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |  Websites belonging to the Company.  |
| Address | {STRING(street), STRING(city), STRING(state), STRING(postal_code), STRING(country)} | OBJECT_BUILDER  |  Company's street, city, state, postal code, and country.  |
| Tags | [STRING] | ARRAY_BUILDER  |  Tags associated with the Company  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(street), STRING(city), STRING(state), STRING(postal_code), STRING(country)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(number), STRING(category)}] | ARRAY_BUILDER  |
| [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |
| [STRING] | ARRAY_BUILDER  |
| [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |





### Create person
Creates a new Person

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The first and last name of the Person.  |
| Emails | [{STRING(email), STRING(category)}($Email)] | ARRAY_BUILDER  |  Email addresses belonging to the Person.  |
| Assignee | STRING | SELECT  |  User that will be the owner of the Person.  |
| Title | STRING | TEXT  |  The professional title of the Person.  |
| Company | STRING | SELECT  |  Primary Company with which the Person is associated.  |
| Contact type | STRING | SELECT  |  The unique identifier of the Contact Type of the Person.  |
| Details | STRING | TEXT  |  Description of the person.  |
| Phone numbers | [{STRING(number), STRING(category)}] | ARRAY_BUILDER  |  Phone numbers belonging to the person.  |
| Socials | [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |  Social profiles belonging to the Person.  |
| Websites | [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |  Websites belonging to the Person.  |
| Address | {STRING(street), STRING(city), STRING(state), STRING(postal_code), STRING(country)} | OBJECT_BUILDER  |  Person's street, city, state, postal code, and country.  |
| Tags | [STRING] | ARRAY_BUILDER  |  Tags associated with the Person.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(street), STRING(city), STRING(state), STRING(postal_code), STRING(country)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(email), STRING(category)}] | ARRAY_BUILDER  |
| [{STRING(number), STRING(category)}] | ARRAY_BUILDER  |
| [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| [{STRING(url), STRING(category)}] | ARRAY_BUILDER  |





