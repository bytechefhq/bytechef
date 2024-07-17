---
title: "Teamwork"
description: "Teamwork is a project management software that helps teams collaborate, organize tasks, and track progress efficiently."
---
## Reference
<hr />

Teamwork is a project management software that helps teams collaborate, organize tasks, and track progress efficiently.


Categories: [CRM, PROJECT_MANAGEMENT]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Your site name | STRING | TEXT  |  e.g. https://{yourSiteName}.teamwork.com  |
| API Key | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Creates a company
Create a new company

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company | {{STRING(name), STRING(emailOne), STRING(phone), STRING(website)}(company)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(name), STRING(emailOne), STRING(phone), STRING(website)} | OBJECT_BUILDER  |





### Creates a task
Create a new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Tasklist Id | INTEGER | SELECT  |  Task list where new task is added  |
| Task | {{STRING(name), STRING(description), DATE(dueAt)}(task)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(name), STRING(description), STRING(dueAt)} | OBJECT_BUILDER  |





