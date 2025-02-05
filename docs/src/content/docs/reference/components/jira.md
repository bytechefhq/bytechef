---
title: "Jira"
description: "Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management."
---

Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management.


Categories: project-management


Type: jira/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| yourDomain | Your domain | STRING | TEXT  |  e.g https://{yourDomain}}.atlassian.net  |  true  |
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Issue
Creates a new issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project | Project ID | STRING | SELECT  |  ID of the project to create the issue in.  |  true  |
| summary | Summary | STRING | TEXT  |  A brief summary of the issue.  |  true  |
| issuetype | Issue Type ID | STRING | SELECT  |  Id of the issue type.  |  true  |
| parent | Parent Issue ID | STRING | SELECT  |  ID of the parent issue.  |  true  |
| assignee | Assignee ID | STRING | SELECT  |  ID of the user who will be assigned to the issue.  |  false  |
| priority | Priority ID | STRING | SELECT  |  ID of the priority of the issue.  |  false  |
| description | Description | STRING | TEXT_AREA  |  Description of the issue.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| key | STRING | TEXT  |
| fields | {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |






### Create Issue Comment
Adds a comment to an issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project | Project ID | STRING | SELECT  |  ID of the project where the issue is located.  |  true  |
| issueId | Issue ID | STRING | SELECT  |  ID of the issue where the comment will be added.  |  true  |
| comment | Comment | STRING | TEXT  |  The text of the comment.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| self | STRING | TEXT  |
| id | STRING | TEXT  |
| author | {STRING\(accountId), BOOLEAN\(active), STRING\(displayName), STRING\(self)} | OBJECT_BUILDER  |
| body | STRING | TEXT  |
| updateAuthor | {STRING\(accountId), BOOLEAN\(active), STRING\(displayName), STRING\(self)} | OBJECT_BUILDER  |
| created | STRING | TEXT  |
| updated | STRING | TEXT  |
| visibility | {STRING\(identifier), STRING\(type), STRING\(value)} | OBJECT_BUILDER  |






### Get Issue
Get issue details in selected project.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project | Project ID | STRING | SELECT  |  ID of the project where the issue is located.  |  true  |
| issueId | Issue ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| key | STRING | TEXT  |
| fields | {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |






### Search Issues
Search for issues using JQL

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| jql | JQL | STRING | TEXT  |  The JQL that defines the search. If no JQL expression is provided, all issues are returned  |  false  |
| maxResults | Max Results | INTEGER | INTEGER  |  The maximum number of items to return per page.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(key), {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)}\(fields)} | OBJECT_BUILDER  |








## Triggers


### New Issue
Triggers when a new issue is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project | Project ID | STRING | SELECT  |  Id of the project where new issue is created.  |  true  |
| issuetype | Issue Type ID | STRING | SELECT  |  ID of the issue type.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| key | STRING | TEXT  |
| fields | {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |







### Updated Issue
Triggers when an issue is updated.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project | Project ID | STRING | SELECT  |  ID of the project where issues is updated.  |  true  |
| issuetype | Issue Type ID | STRING | SELECT  |  ID of the issue type.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| key | STRING | TEXT  |
| fields | {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} | OBJECT_BUILDER  |







<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)"><iframe src="https://www.guidejar.com/embed/c1894615-16ae-48ae-9706-7bf831aa8963?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
