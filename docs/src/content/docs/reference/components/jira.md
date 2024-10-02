---
title: "Jira"
description: "Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management."
---
## Reference
<hr />

Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management.


Categories: [project-management]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Your domain | STRING | TEXT  |  e.g https://{yourDomain}}.atlassian.net  |
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers


### New Issue
Triggers when a new issue is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | STRING | SELECT  |  Project where new issue is created.  |
| Issue type | STRING | SELECT  |  The type of issue.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |







### Updated Issue
Triggers when an issue is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | STRING | SELECT  |  Project where new issue is created.  |
| Issue type | STRING | SELECT  |  The type of issue.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |







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
| {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |






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
| {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |






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
| {STRING\(id), STRING\(key), {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)}\(fields)} | OBJECT_BUILDER  |






<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)"><iframe src="https://www.guidejar.com/embed/c1894615-16ae-48ae-9706-7bf831aa8963?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
