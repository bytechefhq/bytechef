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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| content | Content | STRING | Task content. It may contain some markdown-formatted text and hyperlinks. | true |
| description | Description | STRING | A description for the task. This value may contain some markdown-formatted text and hyperlinks. | false |
| project_id | Project ID | STRING | Task project ID. If not set, task is put to user's Inbox. | false |
| priority | Priority | INTEGER <details> <summary> Options </summary> 1, 2, 3, 4 </details> | Task priority from 1 (normal) to 4 (urgent). | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| project_id | STRING |  |
| content | STRING |  |
| description | STRING |  |
| priority | INTEGER |  |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "content" : "",
    "description" : "",
    "project_id" : "",
    "priority" : 1
  },
  "type" : "todoist/v1/createTask"
}
```


### Mark Task as Completed
Name: markTaskCompleted

Mark a tas as being completed.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| taskId | Task ID | STRING | ID of the task to be closed. | true |


#### Output

This action does not produce any output.

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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | Name of the project. | true |
| color | Color | STRING <details> <summary> Options </summary> beryy_red, red, orange, yellow, olive_green, lime_green, green, mint_green, teal, sky_blue, light_blue, blue, grape, violet, lavender, magenta, salmon, charcoal, grey, taupe </details> |  | false |
| is_favorite | Is Project a Favorite? | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether the project is a favorite. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |
| color | STRING |  |
| is_favorite | STRING |  |
| url | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Project",
  "name" : "createProject",
  "parameters" : {
    "name" : "",
    "color" : "",
    "is_favorite" : false
  },
  "type" : "todoist/v1/createProject"
}
```




