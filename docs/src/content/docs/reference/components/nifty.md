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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Project
Name: createProject

Creates new project.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Project | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(description), STRING\(template_id)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(description), STRING\(template_id)} </details> |




#### JSON Example
```json
{
  "label" : "Create Project",
  "name" : "createProject",
  "parameters" : {
    "__item" : {
      "name" : "",
      "description" : "",
      "template_id" : ""
    }
  },
  "type" : "nifty/v1/createProject"
}
```


### Create Task
Name: createTask

Creates new task

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project | Project ID | STRING | ID of the project within which the task will be created. | true |
| __item | Task | OBJECT <details> <summary> Properties </summary> {STRING\(task_group_id), STRING\(name), STRING\(description), DATE_TIME\(due_date)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(project), STRING\(description), DATE_TIME\(due_date)} </details> |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "project" : "",
    "__item" : {
      "task_group_id" : "",
      "name" : "",
      "description" : "",
      "due_date" : "2021-01-01T00:00:00"
    }
  },
  "type" : "nifty/v1/createTask"
}
```


### Get Task
Name: getTask

Gets task details.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| taskId | Task ID | STRING | ID of the task to get details for. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(project), STRING\(description)} </details> |




#### JSON Example
```json
{
  "label" : "Get Task",
  "name" : "getTask",
  "parameters" : {
    "taskId" : ""
  },
  "type" : "nifty/v1/getTask"
}
```


### Create Status
Name: createStatus

Creates new status

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Status | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(project_id)} </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(message), {STRING\(id), STRING\(name), STRING\(color), STRING\(created_by), STRING\(project), INTEGER\(order)}\(task_group)} </details> |




#### JSON Example
```json
{
  "label" : "Create Status",
  "name" : "createStatus",
  "parameters" : {
    "__item" : {
      "name" : "",
      "project_id" : ""
    }
  },
  "type" : "nifty/v1/createStatus"
}
```


### Get Tracked Time Report
Name: getTrackedTimeReport

Gets tracked time report information.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project_id | Project ID | STRING | Id of the project to get the report for. | true |
| start_date | Start Date | DATE_TIME | Start date for the report. | false |
| end_date | End Date | DATE_TIME | Start date for the report. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{STRING\(id), STRING\(project), STRING\(start), BOOLEAN\(manual), STRING\(user), STRING\(task), STRING\(end), BOOLEAN\(active), STRING\(duration)}]\(items)} </details> |




#### JSON Example
```json
{
  "label" : "Get Tracked Time Report",
  "name" : "getTrackedTimeReport",
  "parameters" : {
    "project_id" : "",
    "start_date" : "2021-01-01T00:00:00",
    "end_date" : "2021-01-01T00:00:00"
  },
  "type" : "nifty/v1/getTrackedTimeReport"
}
```




## Triggers


### New Task
Name: newTask

Triggers when new task is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| app_id | Application | STRING | Application to be used for the trigger. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| project | STRING |
| order | STRING |
| milestone | STRING |




#### JSON Example
```json
{
  "label" : "New Task",
  "name" : "newTask",
  "parameters" : {
    "app_id" : ""
  },
  "type" : "nifty/v1/newTask"
}
```


<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.89288507% + 32px)"><iframe src="https://www.guidejar.com/embed/d345f40c-f9ff-4717-895a-0449d23bb3e1?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
