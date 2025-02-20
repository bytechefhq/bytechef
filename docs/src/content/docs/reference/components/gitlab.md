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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Issue
Name: createIssue

Creates a new project issue.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| projectId | Project ID | STRING | SELECT | ID of the project where new issue will be created. | true |
| title | Title | STRING | TEXT | The title of an issue. | true |
| description | Description | STRING | TEXT | The description of an issue. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), INTEGER\(iid), INTEGER\(project_id), STRING\(title), STRING\(description), STRING\(web_url)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| projectId | Project ID | STRING | SELECT |  | true |
| issueId | Issue ID | INTEGER <details> <summary> Depends On </summary> projectId </details> | SELECT | ID of the issue to comment on. | true |
| body | Comment | STRING | TEXT | The comment to add to the issue. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(body)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| projectId | Project | STRING | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| description | STRING | TEXT |
| id | INTEGER | INTEGER |
| iid | INTEGER | INTEGER |
| projectId | INTEGER | INTEGER |
| title | STRING | TEXT |




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

