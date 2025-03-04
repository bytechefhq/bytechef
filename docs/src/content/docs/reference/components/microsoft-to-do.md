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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |
| tenantId | Tenant Id | STRING |  | true |





<hr />



## Actions


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| taskListId | Task List ID | STRING | ID of the task list where the task will be created. | true |
| title | Title | STRING | Title of the task. | true |
| importance | Importance | STRING <details> <summary> Options </summary> low, normal, high </details> | Importance of the task | false |
| isReminderOn | Reminder | BOOLEAN <details> <summary> Options </summary> true, false </details> | Set to true if an alert is set to remind the user of the task. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| @odata.etag | STRING |  |
| importance | STRING | Importance of the task. |
| isReminderOn | BOOLEAN <details> <summary> Options </summary> true, false </details> | Indicates whether an alert is set to reminder the user of the task. |
| status | STRING | State or progress of the task. |
| title | STRING | Title of the task. |
| categories | STRING | The categories associated with the task. |
| id | STRING | ID of the task. |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(content), STRING\(contentType)} </details> | Body of the task containing information about the task. |
| linkedResources | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(webUrl), STRING\(applicationName), STRING\(displayName)} </details> |  |




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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| displayName | Title | STRING | Title of the task list. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| @odata.context | STRING |  |
| @odata.etag | STRING |  |
| id | STRING |  |
| displayName | STRING |  |
| isOwner | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |
| isShared | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |
| wellKnownListName | STRING |  |




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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| taskListId | Task List ID | STRING | ID of the task list where the task will be created. | true |
| taskId | Task ID | STRING <details> <summary> Depends On </summary> taskListId </details> | ID of the task to retrieve. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| @odata.etag | STRING |  |
| importance | STRING | Importance of the task. |
| isReminderOn | BOOLEAN <details> <summary> Options </summary> true, false </details> | Indicates whether an alert is set to reminder the user of the task. |
| status | STRING | State or progress of the task. |
| title | STRING | Title of the task. |
| categories | STRING | The categories associated with the task. |
| id | STRING | ID of the task. |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(content), STRING\(contentType)} </details> | Body of the task containing information about the task. |
| linkedResources | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(webUrl), STRING\(applicationName), STRING\(displayName)} </details> |  |




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




