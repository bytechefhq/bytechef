---
title: "Asana"
description: "Asana is a web and mobile application designed to help teams organize, track, and manage their work tasks and projects efficiently."
---
## Reference
<hr />

Asana is a web and mobile application designed to help teams organize, track, and manage their work tasks and projects efficiently.


Categories: [PROJECT_MANAGEMENT]


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


### Create project
Creates a new project in a workspace or team.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | {{STRING(workspace), STRING(name), STRING(notes), STRING(team)}(data)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(gid), STRING(name), STRING(notes), {STRING(gid), STRING(name)}(team), {STRING(gid), STRING(name)}(workspace)} | OBJECT_BUILDER  |





### Create a task
Creates a new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | {{STRING(workspace), STRING(project), STRING(name), STRING(notes), DATE(due_on), [STRING](tags), STRING(assignee)}(data)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(gid), DATE(due_on), STRING(notes), STRING(name), {STRING(gid), STRING(name)}(workspace), [{STRING(gid), STRING(name)}](tags), {STRING(gid), STRING(name)}(assignee)} | OBJECT_BUILDER  |





