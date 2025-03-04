---
title: "ClickUp"
description: "ClickUp is a cloud-based collaboration tool that offers task management, document sharing, goal tracking, and other productivity features for teams."
---

ClickUp is a cloud-based collaboration tool that offers task management, document sharing, goal tracking, and other productivity features for teams.


Categories: project-management


Type: clickup/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create List
Name: createList

Creates a new List in specified Folder.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING |  | true |
| spaceId | Space ID | STRING <details> <summary> Depends On </summary> workspaceId </details> |  | true |
| folderId | Folder ID | STRING <details> <summary> Depends On </summary> spaceId, workspaceId </details> | ID of the folder where new list will be created. | true |
| name | Name | STRING | The name of the list. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |
| folder | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create List",
  "name" : "createList",
  "parameters" : {
    "workspaceId" : "",
    "spaceId" : "",
    "folderId" : "",
    "name" : ""
  },
  "type" : "clickup/v1/createList"
}
```


### Create Task
Name: createTask

Create a new task in a ClickUp workspace and list.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING |  | true |
| spaceId | Space ID | STRING <details> <summary> Depends On </summary> workspaceId </details> |  | true |
| folderId | Folder ID | STRING <details> <summary> Depends On </summary> spaceId, workspaceId </details> |  | false |
| listId | List ID | STRING <details> <summary> Depends On </summary> folderId, spaceId, workspaceId </details> | ID of the list where new task will be created. | true |
| name | Name | STRING | The name of the task. | true |
| description | Description | STRING | The description of task. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |
| description | STRING |  |
| url | STRING |  |
| list | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |
| folder | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "workspaceId" : "",
    "spaceId" : "",
    "folderId" : "",
    "listId" : "",
    "name" : "",
    "description" : ""
  },
  "type" : "clickup/v1/createTask"
}
```


### Create Folder
Name: createFolder

Creates a new folder in a ClickUp workspace.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING |  | true |
| spaceId | Space ID | STRING <details> <summary> Depends On </summary> workspaceId </details> | ID of the space where new folder will be created. | true |
| name | Name | STRING | The name of the folder. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create Folder",
  "name" : "createFolder",
  "parameters" : {
    "workspaceId" : "",
    "spaceId" : "",
    "name" : ""
  },
  "type" : "clickup/v1/createFolder"
}
```




## Triggers


### New List
Name: newList

Triggers when new list is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |
| folder | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |




#### JSON Example
```json
{
  "label" : "New List",
  "name" : "newList",
  "parameters" : {
    "workspaceId" : ""
  },
  "type" : "clickup/v1/newList"
}
```


### New Task
Name: newTask

Triggers when new task is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |
| description | STRING |  |
| url | STRING |  |
| list | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |
| folder | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> |  |




#### JSON Example
```json
{
  "label" : "New Task",
  "name" : "newTask",
  "parameters" : {
    "workspaceId" : ""
  },
  "type" : "clickup/v1/newTask"
}
```


<hr />

