---
title: "Google Tasks"
description: "Google Tasks is a cloud-based task management tool that allows users to create, edit, and organize to-do lists, set deadlines, and track tasks across devices in real-time."
---

Google Tasks is a cloud-based task management tool that allows users to create, edit, and organize to-do lists, set deadlines, and track tasks across devices in real-time.


Categories: productivity-and-collaboration


Type: googleTasks/v1

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
Creates a new task on the specified task list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Title | STRING | TEXT  |  Title of the new task to be created.  |  true  |
| listId | List ID | STRING | SELECT  |  ID of the list where the new task will be stored.  |  true  |
| status | Status | STRING | SELECT  |  Status of the task.  |  true  |
| notes | Notes | STRING | TEXT  |  Notes describing the task.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| title | STRING | TEXT  |
| notes | STRING | TEXT  |
| status | STRING | TEXT  |






### List Task
Returns all tasks in the specified task list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| listId | List ID | STRING | SELECT  |  ID of the list where tasks are stored.  |  true  |
| showCompleted | Show completed | BOOLEAN | SELECT  |  Show also completed tasks. By default both completed task and task that needs action will be shown.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(title), STRING\(notes), STRING\(status)} | OBJECT_BUILDER  |






### Update Task
Updates a specific task on the specified task list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| listId | List ID | STRING | SELECT  |  ID of the list where specific task is stored.  |  true  |
| taskId | Task ID | STRING | SELECT  |  ID of the task to update.  |  true  |
| title | Title | STRING | TEXT  |  Title of the task to be updated. If empty, title will not be changed.  |  false  |
| status | Status | STRING | SELECT  |  Status of the task. If empty, status will not be changed.  |  false  |
| notes | Notes | STRING | TEXT  |  Notes describing the task. If empty, notes will not be changed.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| title | STRING | TEXT  |
| notes | STRING | TEXT  |
| status | STRING | TEXT  |








