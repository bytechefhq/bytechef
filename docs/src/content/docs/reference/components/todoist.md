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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(content), STRING\(description), STRING\(project_id), INTEGER\(priority)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(project_id), STRING\(content), STRING\(description), INTEGER\(priority)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "__item" : {
      "content" : "",
      "description" : "",
      "project_id" : "",
      "priority" : 1
    }
  },
  "type" : "todoist/v1/createTask"
}
```


### Mark Task as Completed
Name: markTaskCompleted

Mark a tas as being completed.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| taskId | Task ID | STRING | SELECT | ID of the task to be closed. | true |


#### JSON Example
```json
{
  "label" : "Mark Task as Completed",
  "name" : "markTaskCompleted",
  "parameters" : {
    "taskId" : ""
  },
  "type" : "todoist/v1/markTaskCompleted"
}
```


### Create Project
Name: createProject

Creates a new project.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Project | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(color), BOOLEAN\(is_favorite)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(color), STRING\(is_favorite), STRING\(url)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Project",
  "name" : "createProject",
  "parameters" : {
    "__item" : {
      "name" : "",
      "color" : "",
      "is_favorite" : false
    }
  },
  "type" : "todoist/v1/createProject"
}
```




