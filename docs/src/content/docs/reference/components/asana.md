---
title: "Asana"
description: "Asana is a web and mobile application designed to help teams organize, track, and manage their work tasks and projects efficiently."
---

Asana is a web and mobile application designed to help teams organize, track, and manage their work tasks and projects efficiently.


Categories: project-management


Type: asana/v1

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


### Create Project
Name: createProject

Creates a new project in a workspace or team.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Project | OBJECT <details> <summary> Properties </summary> {{STRING\(workspace), STRING\(name), STRING\(notes), STRING\(team)}\(data)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| data | OBJECT <details> <summary> Properties </summary> {STRING\(gid), STRING\(name), STRING\(notes), {STRING\(gid), STRING\(name)}\(team), {STRING\(gid), STRING\(name)}\(workspace)} </details> |




#### JSON Example
```json
{
  "label" : "Create Project",
  "name" : "createProject",
  "parameters" : {
    "__item" : {
      "data" : {
        "workspace" : "",
        "name" : "",
        "notes" : "",
        "team" : ""
      }
    }
  },
  "type" : "asana/v1/createProject"
}
```


### Create Task
Name: createTask

Creates a new task

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Task | OBJECT <details> <summary> Properties </summary> {{STRING\(workspace), STRING\(project), STRING\(name), STRING\(notes), DATE\(due_on), [STRING]\(tags), STRING\(assignee)}\(data)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| data | OBJECT <details> <summary> Properties </summary> {STRING\(gid), DATE\(due_on), STRING\(notes), STRING\(name), {STRING\(gid), STRING\(name)}\(workspace), [{STRING\(gid), STRING\(name)}]\(tags), {STRING\(gid), STRING\(name)}\(assignee)} </details> |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "__item" : {
      "data" : {
        "workspace" : "",
        "project" : "",
        "name" : "",
        "notes" : "",
        "due_on" : "2021-01-01",
        "tags" : [ "" ],
        "assignee" : ""
      }
    }
  },
  "type" : "asana/v1/createTask"
}
```




