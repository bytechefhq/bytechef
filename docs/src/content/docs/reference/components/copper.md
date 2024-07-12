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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Email address | STRING | TEXT  |
| Key | STRING | TEXT  |





<hr />





## Actions


### Create activity
Creates a new Activity

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Activity type | STRING | SELECT  |
| Details | STRING | TEXT  |
| Parent type | STRING | SELECT  |
| Parent name | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Create company
Creates a new Company

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Name | STRING | TEXT  |
| Assignee | STRING | SELECT  |
| Email domain | STRING | TEXT  |
| Contact type | STRING | SELECT  |
| Details | STRING | TEXT  |
| Phone numbers | ARRAY | ARRAY_BUILDER  |
| Socials | ARRAY | ARRAY_BUILDER  |
| Websites | ARRAY | ARRAY_BUILDER  |
| Address | OBJECT | OBJECT_BUILDER  |
| Tags | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |





### Create person
Creates a new Person

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Name | STRING | TEXT  |
| Emails | ARRAY | ARRAY_BUILDER  |
| Assignee | STRING | SELECT  |
| Title | STRING | TEXT  |
| Company | STRING | SELECT  |
| Contact type | STRING | SELECT  |
| Details | STRING | TEXT  |
| Phone numbers | ARRAY | ARRAY_BUILDER  |
| Socials | ARRAY | ARRAY_BUILDER  |
| Websites | ARRAY | ARRAY_BUILDER  |
| Address | OBJECT | OBJECT_BUILDER  |
| Tags | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |





