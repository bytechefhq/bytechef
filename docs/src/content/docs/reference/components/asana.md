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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Project
Creates a new project in a workspace or team.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Project | {{STRING\(workspace), STRING\(name), STRING\(notes), STRING\(team)}\(data)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| data | {STRING\(gid), STRING\(name), STRING\(notes), {STRING\(gid), STRING\(name)}\(team), {STRING\(gid), STRING\(name)}\(workspace)} | OBJECT_BUILDER  |






### Create Task
Creates a new task

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Task | {{STRING\(workspace), STRING\(project), STRING\(name), STRING\(notes), DATE\(due_on), [STRING]\(tags), STRING\(assignee)}\(data)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| data | {STRING\(gid), DATE\(due_on), STRING\(notes), STRING\(name), {STRING\(gid), STRING\(name)}\(workspace), [{STRING\(gid), STRING\(name)}]\(tags), {STRING\(gid), STRING\(name)}\(assignee)} | OBJECT_BUILDER  |








## Triggers



<hr />

