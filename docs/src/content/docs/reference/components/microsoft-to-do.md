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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| tenantId | Tenant Id | STRING | TEXT |  | true |





<hr />



## Actions


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| taskListId | Task List ID | STRING | SELECT | ID of the task list where the task will be created. | true |
| title | Title | STRING | TEXT | Title of the task. | true |
| importance | Importance | STRING <details> <summary> Options </summary> low, normal, high </details> | SELECT | Importance of the task | false |
| isReminderOn | Reminder | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Set to true if an alert is set to remind the user of the task. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| @odata.etag | STRING | TEXT |
| importance | STRING | TEXT |
| isReminderOn | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| status | STRING | TEXT |
| title | STRING | TEXT |
| categories | STRING | TEXT |
| id | STRING | TEXT |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(content), STRING\(contentType)} </details> | OBJECT_BUILDER |
| linkedResources | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(webUrl), STRING\(applicationName), STRING\(displayName)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "taskListId" : "",
    "title" : "",
    "importance" : "",
    "isReminderOn" : false
  },
  "type" : "microsoftToDo/v1/createTask"
}
```


### Create Task List
Name: createTaskList

Creates a new task list.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| displayName | Title | STRING | TEXT | Title of the task list. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| @odata.context | STRING | TEXT |
| @odata.etag | STRING | TEXT |
| id | STRING | TEXT |
| displayName | STRING | TEXT |
| isOwner | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| isShared | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| wellKnownListName | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Task List",
  "name" : "createTaskList",
  "parameters" : {
    "displayName" : ""
  },
  "type" : "microsoftToDo/v1/createTaskList"
}
```


### Get Task
Name: getTask

Gets task by ID.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| taskListId | Task List ID | STRING | SELECT | ID of the task list where the task will be created. | true |
| taskId | Task ID | STRING <details> <summary> Depends On </summary> taskListId </details> | SELECT | ID of the task to retrieve. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| @odata.etag | STRING | TEXT |
| importance | STRING | TEXT |
| isReminderOn | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| status | STRING | TEXT |
| title | STRING | TEXT |
| categories | STRING | TEXT |
| id | STRING | TEXT |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(content), STRING\(contentType)} </details> | OBJECT_BUILDER |
| linkedResources | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(webUrl), STRING\(applicationName), STRING\(displayName)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Task",
  "name" : "getTask",
  "parameters" : {
    "taskListId" : "",
    "taskId" : ""
  },
  "type" : "microsoftToDo/v1/getTask"
}
```




