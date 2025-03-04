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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteName | Your site name | STRING | e.g. https://{yourSiteName}.teamwork.com | true |
| username | API Key | STRING |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| company | Company | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(emailOne), STRING\(phone), STRING\(website)} </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| name | STRING |  |
| emailOne | STRING |  |
| phone | STRING |  |
| website | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "company" : {
      "name" : "",
      "emailOne" : "",
      "phone" : "",
      "website" : ""
    }
  },
  "type" : "teamwork/v1/createCompany"
}
```


### Create Task
Name: createTask

Create a new task

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| tasklistId | Task List ID | INTEGER | Task list where new task is added | true |
| task | Task | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(description), DATE\(dueAt)} </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| name | STRING |  |
| description | STRING |  |
| dueAt | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "tasklistId" : 1,
    "task" : {
      "name" : "",
      "description" : "",
      "dueAt" : "2021-01-01"
    }
  },
  "type" : "teamwork/v1/createTask"
}
```




