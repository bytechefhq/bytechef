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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| yourDomain | Your domain | STRING | e.g https://{yourDomain}}.atlassian.net | true |
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Issue
Name: createIssue

Creates a new issue.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project | Project ID | STRING | ID of the project to create the issue in. | true |
| summary | Summary | STRING | A brief summary of the issue. | true |
| issuetype | Issue Type ID | STRING <details> <summary> Depends On </summary> project </details> | Id of the issue type. | true |
| parent | Parent Issue ID | STRING <details> <summary> Depends On </summary> project </details> | ID of the parent issue. | true |
| assignee | Assignee ID | STRING <details> <summary> Depends On </summary> project </details> | ID of the user who will be assigned to the issue. | false |
| priority | Priority ID | STRING | ID of the priority of the issue. | false |
| description | Description | STRING | Description of the issue. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| key | STRING |  |
| fields | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create Issue",
  "name" : "createIssue",
  "parameters" : {
    "project" : "",
    "summary" : "",
    "issuetype" : "",
    "parent" : "",
    "assignee" : "",
    "priority" : "",
    "description" : ""
  },
  "type" : "jira/v1/createIssue"
}
```


### Create Issue Comment
Name: createIssueComment

Adds a comment to an issue.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project | Project ID | STRING | ID of the project where the issue is located. | true |
| issueId | Issue ID | STRING <details> <summary> Depends On </summary> project </details> | ID of the issue where the comment will be added. | true |
| comment | Comment | STRING | The text of the comment. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| self | STRING |  |
| id | STRING |  |
| author | OBJECT <details> <summary> Properties </summary> {STRING\(accountId), BOOLEAN\(active), STRING\(displayName), STRING\(self)} </details> |  |
| body | STRING |  |
| updateAuthor | OBJECT <details> <summary> Properties </summary> {STRING\(accountId), BOOLEAN\(active), STRING\(displayName), STRING\(self)} </details> |  |
| created | STRING |  |
| updated | STRING |  |
| visibility | OBJECT <details> <summary> Properties </summary> {STRING\(identifier), STRING\(type), STRING\(value)} </details> |  |




#### JSON Example
```json
{
  "label" : "Create Issue Comment",
  "name" : "createIssueComment",
  "parameters" : {
    "project" : "",
    "issueId" : "",
    "comment" : ""
  },
  "type" : "jira/v1/createIssueComment"
}
```


### Get Issue
Name: getIssue

Get issue details in selected project.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project | Project ID | STRING | ID of the project where the issue is located. | true |
| issueId | Issue ID | STRING <details> <summary> Depends On </summary> project </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| key | STRING |  |
| fields | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} </details> |  |




#### JSON Example
```json
{
  "label" : "Get Issue",
  "name" : "getIssue",
  "parameters" : {
    "project" : "",
    "issueId" : ""
  },
  "type" : "jira/v1/getIssue"
}
```


### Search Issues
Name: searchForIssuesUsingJql

Search for issues using JQL

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| jql | JQL | STRING | The JQL that defines the search. If no JQL expression is provided, all issues are returned | false |
| maxResults | Max Results | INTEGER | The maximum number of items to return per page. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(key), {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)}\(fields)} </details> |  |




#### JSON Example
```json
{
  "label" : "Search Issues",
  "name" : "searchForIssuesUsingJql",
  "parameters" : {
    "jql" : "",
    "maxResults" : 1
  },
  "type" : "jira/v1/searchForIssuesUsingJql"
}
```




## Triggers


### New Issue
Name: newIssue

Triggers when a new issue is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project | Project ID | STRING | Id of the project where new issue is created. | true |
| issuetype | Issue Type ID | STRING <details> <summary> Depends On </summary> project </details> | ID of the issue type. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| key | STRING |  |
| fields | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} </details> |  |




#### JSON Example
```json
{
  "label" : "New Issue",
  "name" : "newIssue",
  "parameters" : {
    "project" : "",
    "issuetype" : ""
  },
  "type" : "jira/v1/newIssue"
}
```


### Updated Issue
Name: updatedIssue

Triggers when an issue is updated.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project | Project ID | STRING | ID of the project where issues is updated. | true |
| issuetype | Issue Type ID | STRING <details> <summary> Depends On </summary> project </details> | ID of the issue type. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| key | STRING |  |
| fields | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(name)}\(issuetype), {STRING\(id), STRING\(name)}\(project), {STRING\(id), STRING\(name)}\(priority), {STRING\(id), STRING\(name)}\(assignee), {STRING\(type), [{[{STRING\(text), STRING\(type)}]\(content), STRING\(type)}]\(content)}\(description)} </details> |  |




#### JSON Example
```json
{
  "label" : "Updated Issue",
  "name" : "updatedIssue",
  "parameters" : {
    "project" : "",
    "issuetype" : ""
  },
  "type" : "jira/v1/updatedIssue"
}
```


<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)"><iframe src="https://www.guidejar.com/embed/c1894615-16ae-48ae-9706-7bf831aa8963?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
