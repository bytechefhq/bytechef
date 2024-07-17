---
title: "Nifty"
description: "Nifty Project Management tool is a software designed to aid project managers in organizing, planning, and tracking tasks and resources within a project"
---
## Reference
<hr />

Nifty Project Management tool is a software designed to aid project managers in organizing, planning, and tracking tasks and resources within a project


Categories: [PROJECT_MANAGEMENT]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client id | STRING | TEXT  |  |
| Client secret | STRING | TEXT  |  |





<hr />





## Actions


### Create Task
Create a new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Name of the task  |
| Description | STRING | TEXT  |  Description of the task.  |
| Project | STRING | SELECT  |  Project within which the task will be created.  |
| Status | STRING | SELECT  |  |
| Due date | DATE_TIME | DATE_TIME  |  Due date for the task.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





