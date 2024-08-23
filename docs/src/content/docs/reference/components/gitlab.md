---
title: "Gitlab"
description: "GitLab is a web-based DevOps lifecycle tool that provides a Git repository manager, CI/CD pipelines, issue tracking, and more in a single application."
---
## Reference
<hr />

GitLab is a web-based DevOps lifecycle tool that provides a Git repository manager, CI/CD pipelines, issue tracking, and more in a single application.


Categories: [developer-tools]


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


### New Issue
Triggers when a new issue is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |







<hr />



## Actions


### Create Issue
Creates a new project issue.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | STRING | SELECT  |  Project where new issue will be created  |
| Title | STRING | TEXT  |  The title of an issue.  |
| Description | STRING | TEXT  |  The description of an issue.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {INTEGER\(id), INTEGER\(iid), INTEGER\(project_id), STRING\(title), STRING\(description), STRING\(web_url)} | OBJECT_BUILDER  |






### Create Comment on Issue
Adds a comment to the specified issue.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project | STRING | SELECT  |  |
| Issue | INTEGER | SELECT  |  The issue to comment on.  |
| Comment | STRING | TEXT  |  The comment to add to the issue.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {INTEGER\(id), STRING\(body)} | OBJECT_BUILDER  |






