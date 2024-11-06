---
title: "Affinity"
description: "Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects."
---
## Reference
<hr />

Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects.


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



## Triggers



<hr />



## Actions


### Create Opportunity
Creates a new opportunity.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Opportunity | {STRING\(name)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |






### Create Organization
Creates a new organization.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Organization | {STRING\(name), STRING\(domain)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name), STRING\(domain)} | OBJECT_BUILDER  |






### Create Person
Creates a new person.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Person | {STRING\(first_name), STRING\(last_name), [STRING]\(emails)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(first_name), STRING\(last_name), [STRING]\(emails)} | OBJECT_BUILDER  |






