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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Issue
Creates a new project issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| projectId | Project ID | STRING | SELECT  |  ID of the project where new issue will be created.  |  true  |
| title | Title | STRING | TEXT  |  The title of an issue.  |  true  |
| description | Description | STRING | TEXT  |  The description of an issue.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {INTEGER\(id), INTEGER\(iid), INTEGER\(project_id), STRING\(title), STRING\(description), STRING\(web_url)} | OBJECT_BUILDER  |






### Create Comment on Issue
Adds a comment to the specified issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| projectId | Project ID | STRING | SELECT  |  | true  |
| issueId | Issue ID | INTEGER | SELECT  |  ID of the issue to comment on.  |  true  |
| body | Comment | STRING | TEXT  |  The comment to add to the issue.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {INTEGER\(id), STRING\(body)} | OBJECT_BUILDER  |








## Triggers


### New Issue
Triggers when a new issue is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| projectId | Project | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| description | STRING | TEXT  |
| id | INTEGER | INTEGER  |
| iid | INTEGER | INTEGER  |
| projectId | INTEGER | INTEGER  |
| title | STRING | TEXT  |







<hr />

