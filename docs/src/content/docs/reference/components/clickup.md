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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create List
Creates a new List in specified Folder.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workspaceId | Workspace ID | NUMBER | SELECT  |  | true  |
| spaceId | Space ID | NUMBER | SELECT  |  | true  |
| folderId | Folder ID | NUMBER | SELECT  |  ID of the folder where new list will be created.  |  true  |
| __item | List | {STRING\(name)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), {STRING\(id), STRING\(name)}\(folder), {STRING\(id), STRING\(name)}\(space)} | OBJECT_BUILDER  |






### Create Task
Create a new task in a ClickUp workspace and list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workspaceId | Workspace ID | NUMBER | SELECT  |  | true  |
| spaceId | Space ID | NUMBER | SELECT  |  | true  |
| folderId | Folder ID | NUMBER | SELECT  |  | false  |
| listId | List ID | NUMBER | SELECT  |  ID of the list where new task will be created.  |  true  |
| __item | Task | {STRING\(name), STRING\(description)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), STRING\(description), STRING\(url), {STRING\(id), STRING\(name)}\(list), {STRING\(id), STRING\(name)}\(folder), {STRING\(id)}\(space)} | OBJECT_BUILDER  |






### Create Folder
Creates a new folder in a ClickUp workspace.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workspaceId | Workspace ID | NUMBER | SELECT  |  | true  |
| spaceId | Space ID | NUMBER | SELECT  |  ID of the space where new folder will be created.  |  true  |
| __item | Folder | {STRING\(name)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), {STRING\(id), STRING\(name)}\(space)} | OBJECT_BUILDER  |








## Triggers


### New List
Triggers when new list is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workspaceId | Workspace ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |
| folder | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |
| space | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







### New Task
Triggers when new task is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| workspaceId | Workspace ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |
| description | STRING | TEXT  |
| url | STRING | TEXT  |
| list | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |
| folder | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |
| space | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







<hr />

