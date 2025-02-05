---
title: "Teamwork"
description: "Teamwork is a project management software that helps teams collaborate, organize tasks, and track progress efficiently."
---

Teamwork is a project management software that helps teams collaborate, organize tasks, and track progress efficiently.


Categories: crm, project-management


Type: teamwork/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteName | Your site name | STRING | TEXT  |  e.g. https://{yourSiteName}.teamwork.com  |  true  |
| username | API Key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Company
Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Company | {{STRING\(name), STRING\(emailOne), STRING\(phone), STRING\(website)}\(company)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(name), STRING\(emailOne), STRING\(phone), STRING\(website)} | OBJECT_BUILDER  |






### Create Task
Create a new task

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tasklistId | Task List ID | INTEGER | SELECT  |  Task list where new task is added  |  true  |
| __item | Task | {{STRING\(name), STRING\(description), DATE\(dueAt)}\(task)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(name), STRING\(description), STRING\(dueAt)} | OBJECT_BUILDER  |








## Triggers



<hr />

