---
title: "Todoist"
description: "Todoist is a task management application that helps users organize and prioritize their to-do lists."
---
## Reference
<hr />

Todoist is a task management application that helps users organize and prioritize their to-do lists.


Categories: [PRODUCTIVITY_AND_COLLABORATION]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create Task
Creates a new Task.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {STRING\(content), STRING\(description), STRING\(project_id), INTEGER\(priority)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(project_id), STRING\(content), STRING\(description), INTEGER\(priority)} | OBJECT_BUILDER  |






### Mark Task as Completed
Mark a tas as being completed.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | STRING | SELECT  |  Task to be closed.  |




### Create Project
Creates a new Project.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | {STRING\(name), STRING\(color), BOOLEAN\(is_favorite)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name), STRING\(color), STRING\(is_favorite), STRING\(url)} | OBJECT_BUILDER  |






