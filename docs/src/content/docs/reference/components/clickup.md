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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create List
Name: createList

Creates a new List in specified Folder.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | NUMBER | SELECT |  | true |
| spaceId | Space ID | NUMBER <details> <summary> Depends On </summary> workspaceId </details> | SELECT |  | true |
| folderId | Folder ID | NUMBER <details> <summary> Depends On </summary> spaceId, workspaceId </details> | SELECT | ID of the folder where new list will be created. | true |
| __item | List | OBJECT <details> <summary> Properties </summary> {STRING\(name)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), {STRING\(id), STRING\(name)}\(folder), {STRING\(id), STRING\(name)}\(space)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create List",
  "name" : "createList",
  "parameters" : {
    "workspaceId" : 0.0,
    "spaceId" : 0.0,
    "folderId" : 0.0,
    "__item" : {
      "name" : ""
    }
  },
  "type" : "clickup/v1/createList"
}
```


### Create Task
Name: createTask

Create a new task in a ClickUp workspace and list.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | NUMBER | SELECT |  | true |
| spaceId | Space ID | NUMBER <details> <summary> Depends On </summary> workspaceId </details> | SELECT |  | true |
| folderId | Folder ID | NUMBER <details> <summary> Depends On </summary> spaceId, workspaceId </details> | SELECT |  | false |
| listId | List ID | NUMBER <details> <summary> Depends On </summary> folderId, spaceId, workspaceId </details> | SELECT | ID of the list where new task will be created. | true |
| __item | Task | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(description)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(description), STRING\(url), {STRING\(id), STRING\(name)}\(list), {STRING\(id), STRING\(name)}\(folder), {STRING\(id)}\(space)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "workspaceId" : 0.0,
    "spaceId" : 0.0,
    "folderId" : 0.0,
    "listId" : 0.0,
    "__item" : {
      "name" : "",
      "description" : ""
    }
  },
  "type" : "clickup/v1/createTask"
}
```


### Create Folder
Name: createFolder

Creates a new folder in a ClickUp workspace.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | NUMBER | SELECT |  | true |
| spaceId | Space ID | NUMBER <details> <summary> Depends On </summary> workspaceId </details> | SELECT | ID of the space where new folder will be created. | true |
| __item | Folder | OBJECT <details> <summary> Properties </summary> {STRING\(name)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), {STRING\(id), STRING\(name)}\(space)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Folder",
  "name" : "createFolder",
  "parameters" : {
    "workspaceId" : 0.0,
    "spaceId" : 0.0,
    "__item" : {
      "name" : ""
    }
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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| name | STRING | TEXT |
| folder | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| workspaceId | Workspace ID | STRING | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| name | STRING | TEXT |
| description | STRING | TEXT |
| url | STRING | TEXT |
| list | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| folder | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




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

