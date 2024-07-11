---
title: "Teamwork"
description: "Teamwork is a project management software that helps teams collaborate, organize tasks, and track progress efficiently."
---
## Reference
<hr />

Teamwork is a project management software that helps teams collaborate, organize tasks, and track progress efficiently.

Categories: [CRM, PROJECT_MANAGEMENT]

Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Your site name | STRING | TEXT  |
| API Key | STRING | TEXT  |





<hr />



## Triggers



<hr />



## Actions


### Creates a company
Create a new company

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Company | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Creates a task
Create a new task

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Tasklist Id | INTEGER | SELECT  |
| Task | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





