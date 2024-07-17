---
title: "Affinity"
description: "Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects."
---
## Reference
<hr />

Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects.


Categories: [CRM]


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


### Create opportunity
Creates a new opportunity

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name of the opportunity.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |





### Create organization
Creates a new organization

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name of the organization.  |
| Domain | STRING | TEXT  |  The domain name of the organization.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |





### Create person
Creates a new person

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First name | STRING | TEXT  |  The first name of the person.  |
| Last name | STRING | TEXT  |  The last name of the person.  |
| Emails | [STRING] | ARRAY_BUILDER  |  The email addresses of the person.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |





