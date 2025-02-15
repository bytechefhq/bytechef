---
title: "Microsoft To Do"
description: "Microsoft To Do is a cloud-based task management application that helps users organize, prioritize, and track tasks across devices with features like lists, reminders, and collaboration."
---

Microsoft To Do is a cloud-based task management application that helps users organize, prioritize, and track tasks across devices with features like lists, reminders, and collaboration.


Categories: productivity-and-collaboration


Type: microsoftToDo/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| tenantId | Tenant Id | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| taskListId | Task List ID | STRING | SELECT  |  ID of the task list where the task will be created.  |  true  |
| title | Title | STRING | TEXT  |  Title of the task.  |  true  |
| importance | Importance | STRING | SELECT  |  Importance of the task  |  false  |
| isReminderOn | Reminder | BOOLEAN | SELECT  |  Set to true if an alert is set to remind the user of the task.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| @odata.etag | STRING | TEXT  |
| importance | STRING | TEXT  |
| isReminderOn | BOOLEAN | SELECT  |
| status | STRING | TEXT  |
| title | STRING | TEXT  |
| categories | STRING | TEXT  |
| id | STRING | TEXT  |
| body | {STRING\(content), STRING\(contentType)} | OBJECT_BUILDER  |
| linkedResources | {STRING\(id), STRING\(webUrl), STRING\(applicationName), STRING\(displayName)} | OBJECT_BUILDER  |






### Create Task List
Name: createTaskList

Creates a new task list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| displayName | Title | STRING | TEXT  |  Title of the task list.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| @odata.context | STRING | TEXT  |
| @odata.etag | STRING | TEXT  |
| id | STRING | TEXT  |
| displayName | STRING | TEXT  |
| isOwner | BOOLEAN | SELECT  |
| isShared | BOOLEAN | SELECT  |
| wellKnownListName | STRING | TEXT  |






### Get Task
Name: getTask

Gets task by ID.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| taskListId | Task List ID | STRING | SELECT  |  ID of the task list where the task will be created.  |  true  |
| taskId | Task ID | STRING | SELECT  |  ID of the task to retrieve.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| @odata.etag | STRING | TEXT  |
| importance | STRING | TEXT  |
| isReminderOn | BOOLEAN | SELECT  |
| status | STRING | TEXT  |
| title | STRING | TEXT  |
| categories | STRING | TEXT  |
| id | STRING | TEXT  |
| body | {STRING\(content), STRING\(contentType)} | OBJECT_BUILDER  |
| linkedResources | {STRING\(id), STRING\(webUrl), STRING\(applicationName), STRING\(displayName)} | OBJECT_BUILDER  |








