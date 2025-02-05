---
title: "Nifty"
description: "Nifty Project Management is a software tool that streamlines team collaboration and project tracking with features like task management, timelines, and communication tools to enhance productivity."
---

Nifty Project Management is a software tool that streamlines team collaboration and project tracking with features like task management, timelines, and communication tools to enhance productivity.


Categories: project-management, productivity-and-collaboration


Type: nifty/v1

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
Creates new project.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Project | {STRING\(name), STRING\(description), STRING\(template_id)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), STRING\(description), STRING\(template_id)} | OBJECT_BUILDER  |






### Create Task
Creates new task

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project | Project ID | STRING | SELECT  |  ID of the project within which the task will be created.  |  true  |
| __item | Task | {STRING\(task_group_id), STRING\(name), STRING\(description), DATE_TIME\(due_date)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), STRING\(project), STRING\(description), DATE_TIME\(due_date)} | OBJECT_BUILDER  |






### Get Task
Gets task details.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| taskId | Task ID | STRING | SELECT  |  ID of the task to get details for.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(name), STRING\(project), STRING\(description)} | OBJECT_BUILDER  |






### Create Status
Creates new status

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Status | {STRING\(name), STRING\(project_id)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(message), {STRING\(id), STRING\(name), STRING\(color), STRING\(created_by), STRING\(project), INTEGER\(order)}\(task_group)} | OBJECT_BUILDER  |






### Get Tracked Time Report
Gets tracked time report information.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project_id | Project ID | STRING | SELECT  |  Id of the project to get the report for.  |  true  |
| start_date | Start Date | DATE_TIME | DATE_TIME  |  Start date for the report.  |  false  |
| end_date | End Date | DATE_TIME | DATE_TIME  |  Start date for the report.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{STRING\(id), STRING\(project), STRING\(start), BOOLEAN\(manual), STRING\(user), STRING\(task), STRING\(end), BOOLEAN\(active), STRING\(duration)}]\(items)} | OBJECT_BUILDER  |








## Triggers


### New Task
Triggers when new task is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| app_id | Application | STRING | SELECT  |  Application to be used for the trigger.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| project | STRING | TEXT  |
| order | STRING | TEXT  |
| milestone | STRING | TEXT  |







<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.89288507% + 32px)"><iframe src="https://www.guidejar.com/embed/d345f40c-f9ff-4717-895a-0449d23bb3e1?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
