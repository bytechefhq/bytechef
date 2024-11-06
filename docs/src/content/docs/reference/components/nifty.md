---
title: "Nifty"
description: "Nifty Project Management is a software tool that streamlines team collaboration and project tracking with features like task management, timelines, and communication tools to enhance productivity."
---
## Reference
<hr />

Nifty Project Management is a software tool that streamlines team collaboration and project tracking with features like task management, timelines, and communication tools to enhance productivity.


Categories: [project-management, productivity-and-collaboration]


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
Creates new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | STRING | SELECT  |  Project within which the task will be created.  |
| Task | {STRING\(task_group_id), STRING\(name), STRING\(description), DATE_TIME\(due_date)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name), STRING\(project), STRING\(description), DATE_TIME\(due_date)} | OBJECT_BUILDER  |






<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.89288507% + 32px)"><iframe src="https://www.guidejar.com/embed/d345f40c-f9ff-4717-895a-0449d23bb3e1?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
