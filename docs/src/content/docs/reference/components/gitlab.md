---
title: "GitLab"
description: "GitLab is a web-based DevOps lifecycle tool that provides a Git repository manager, CI/CD pipelines, issue tracking, and more in a single application."
---

GitLab is a web-based DevOps lifecycle tool that provides a Git repository manager, CI/CD pipelines, issue tracking, and more in a single application.


Categories: developer-tools


Type: gitlab/v1

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


### Create Issue
Name: createIssue

Creates a new project issue.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| projectId | Project ID | STRING | ID of the project where new issue will be created. | true |
| title | Title | STRING | The title of an issue. | true |
| description | Description | STRING | The description of an issue. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| iid | INTEGER |
| project_id | INTEGER |
| title | STRING |
| description | STRING |
| web_url | STRING |




#### JSON Example
```json
{
  "label" : "Create Issue",
  "name" : "createIssue",
  "parameters" : {
    "projectId" : "",
    "title" : "",
    "description" : ""
  },
  "type" : "gitlab/v1/createIssue"
}
```


### Create Comment on Issue
Name: createCommentOnIssue

Adds a comment to the specified issue.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| projectId | Project ID | STRING |  | true |
| issueId | Issue ID | INTEGER <details> <summary> Depends On </summary> projectId </details> | ID of the issue to comment on. | true |
| body | Comment | STRING | The comment to add to the issue. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| body | STRING |




#### JSON Example
```json
{
  "label" : "Create Comment on Issue",
  "name" : "createCommentOnIssue",
  "parameters" : {
    "projectId" : "",
    "issueId" : 1,
    "body" : ""
  },
  "type" : "gitlab/v1/createCommentOnIssue"
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
| projectId | Project | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| description | STRING |
| id | INTEGER |
| iid | INTEGER |
| projectId | INTEGER |
| title | STRING |




#### JSON Example
```json
{
  "label" : "New Issue",
  "name" : "newIssue",
  "parameters" : {
    "projectId" : ""
  },
  "type" : "gitlab/v1/newIssue"
}
```


<hr />

