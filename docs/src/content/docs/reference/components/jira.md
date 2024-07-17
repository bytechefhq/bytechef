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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Your domain | STRING | TEXT  |  e.g https://{yourDomain}}.atlassian.net  |
| Email | STRING | TEXT  |  The email used to log in to Jira  |
| API token | STRING | TEXT  |  |





<hr />





## Actions


### Create issue
Creates a new issue.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project Name | STRING | SELECT  |  The name of the project to create the issue in.  |
| Summary | STRING | TEXT  |  A brief summary of the issue.  |
| Issue type | STRING | SELECT  |  The type of issue.  |
| Parent | STRING | SELECT  |    |
| Assignee | STRING | SELECT  |  User who will be assigned to the issue.  |
| Priority | STRING | SELECT  |  Priority of the issue.  |
| Description | STRING | TEXT_AREA  |  Description of the issue.  |


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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project Name | STRING | SELECT  |  Project where the issue is located.  |
| Issue name | STRING | SELECT  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {{STRING(id), STRING(name)}(issuetype), {STRING(id), STRING(name)}(project), {STRING(id), STRING(name)}(priority), {STRING(id), STRING(name)}(assignee), {STRING(type), [{[{STRING(text), STRING(type)}](content), STRING(type)}](content)}(description)} | OBJECT_BUILDER  |





### Search issues
Search for issues using JQL

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| JQL | STRING | TEXT  |  The JQL that defines the search. If no JQL expression is provided, all issues are returned  |
| Max results | INTEGER | INTEGER  |  The maximum number of items to return per page.  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





