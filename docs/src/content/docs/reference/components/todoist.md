---
title: "Todoist"
description: "Todoist is a task management application that helps users organize and prioritize their to-do lists."
---

Todoist is a task management application that helps users organize and prioritize their to-do lists.


Categories: productivity-and-collaboration


Type: todoist/v1

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


### Create Task
Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {STRING\(content), STRING\(description), STRING\(project_id), INTEGER\(priority)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(project_id), STRING\(content), STRING\(description), INTEGER\(priority)} | OBJECT_BUILDER  |






### Mark Task as Completed
Mark a tas as being completed.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| taskId | Task ID | STRING | SELECT  |  ID of the task to be closed.  |  true  |




### Create Project
Creates a new project.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Project | {STRING\(name), STRING\(color), BOOLEAN\(is_favorite)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), STRING\(color), STRING\(is_favorite), STRING\(url)} | OBJECT_BUILDER  |








## Triggers



<hr />

