---
title: "Jira"
description: "Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management."
---
## Reference
<hr />

Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management.


Categories: [PROJECT_MANAGEMENT]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Your domain | STRING | TEXT  |
| Email | STRING | TEXT  |
| API token | STRING | TEXT  |





<hr />





## Actions


### Create issue
Creates a new issue.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Project Name | STRING | SELECT  |
| Summary | STRING | TEXT  |
| Issue type | STRING | SELECT  |
| Parent | STRING | SELECT  |
| Assignee | STRING | SELECT  |
| Priority | STRING | SELECT  |
| Description | STRING | TEXT_AREA  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |





### Get issue
Get issue details in selected project.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Project Name | STRING | SELECT  |
| Issue name | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Search issues
Search for issues using JQL

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| JQL | STRING | TEXT  |
| Max results | INTEGER | INTEGER  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





