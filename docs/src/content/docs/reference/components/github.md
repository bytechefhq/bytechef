---
title: "Github"
description: "GitHub is a web-based platform for version control and collaboration using Git."
---
## Reference
<hr />

GitHub is a web-based platform for version control and collaboration using Git.


Categories: [DEVELOPER_TOOLS]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client id | STRING | TEXT  |  |
| Client secret | STRING | TEXT  |  |





<hr />



## Triggers


### New Issue
Triggers when a new issue is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(url), STRING\(repository_url), NUMBER\(id), INTEGER\(number), STRING\(title), STRING\(state), STRING\(body)} | OBJECT_BUILDER  |
| {STRING\(login), INTEGER\(id)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| {INTEGER\(id), STRING\(name), STRING\(full_name), {STRING\(login), INTEGER\(id)}\(owner), STRING\(visibility), INTEGER\(forks), INTEGER\(open_issues), STRING\(default_branch)} | OBJECT_BUILDER  |







### New Pull Request
Triggers when a new pull request is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| {INTEGER\(id), STRING\(state), STRING\(title), STRING\(body), INTEGER\(commits)} | OBJECT_BUILDER  |
| {STRING\(login), INTEGER\(id)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| {INTEGER\(id), STRING\(name), STRING\(full_name), {STRING\(login), INTEGER\(id)}\(owner), STRING\(visibility), INTEGER\(forks), INTEGER\(open_issues), STRING\(default_branch)} | OBJECT_BUILDER  |







<hr />



## Actions


### Create issue
Create Issue in GitHub Repository

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  Repository where new issue will be created.  |
| Title | STRING | TEXT  |  Title of the issue.  |
| Description | STRING | TEXT  |  The description of the issue.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Get Issue
Get information from a specific issue

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  |
| Issue | STRING | SELECT  |  The issue you want to get details from.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create comment on a issue
Adds a comment to the specified issue.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  |
| Issue | STRING | SELECT  |  The issue to comment on.  |
| Comment | STRING | TEXT  |  The comment to add to the issue.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






<hr />

# Additional instructions
<hr />

## CONNECTION

[Setting up OAuth2](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)"><iframe src="https://www.guidejar.com/embed/056201df-2b7a-45c7-9691-3aad03b82487?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
