---
title: "GitHub"
description: "GitHub is a web-based platform for version control and collaboration using Git."
---

GitHub is a web-based platform for version control and collaboration using Git.


Categories: developer-tools


Type: github/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client id | STRING | TEXT  |  | true  |
| clientSecret | Client secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Add Assignee to Issue
Name: addAssigneesToIssue

Adds an assignees to the specified issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  | true  |
| issue | Issue | STRING | SELECT  |  The issue to add assignee to.  |  true  |
| assignees | Assignees | [STRING\($assignee)] | ARRAY_BUILDER  |  The list of assignees to add to the issue.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| repository_url | STRING | TEXT  |
| id | NUMBER | NUMBER  |
| number | INTEGER | INTEGER  |
| title | STRING | TEXT  |
| state | STRING | TEXT  |
| assignees | [{STRING\(login), STRING\(id), STRING\(html_url), STRING\(type)}] | ARRAY_BUILDER  |
| labels | [{STRING\(id), STRING\(name), STRING\(description)}] | ARRAY_BUILDER  |
| body | STRING | TEXT  |






### Add Labels to Issue
Name: addLabelsToIssue

Adds labels to the specified issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  | true  |
| issue | Issue | STRING | SELECT  |  The issue to add labels to.  |  true  |
| labels | Labels | [STRING\($label)] | ARRAY_BUILDER  |  The list of labels to add to the issue.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(name), STRING\(description)} | OBJECT_BUILDER  |






### Create Comment on Issue
Name: createCommentOnIssue

Adds a comment to the specified issue.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  | true  |
| issue | Issue | STRING | SELECT  |  The issue to comment on.  |  true  |
| body | Comment | STRING | TEXT  |  The comment to add to the issue.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| repository_url | STRING | TEXT  |
| id | NUMBER | NUMBER  |
| number | INTEGER | INTEGER  |
| title | STRING | TEXT  |
| state | STRING | TEXT  |
| assignees | [{STRING\(login), STRING\(id), STRING\(html_url), STRING\(type)}] | ARRAY_BUILDER  |
| labels | [{STRING\(id), STRING\(name), STRING\(description)}] | ARRAY_BUILDER  |
| body | STRING | TEXT  |






### Create Issue
Name: createIssue

Create Issue in GitHub Repository

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  Repository where new issue will be created.  |  true  |
| title | Title | STRING | TEXT  |  Title of the issue.  |  false  |
| body | Description | STRING | TEXT  |  The description of the issue.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| repository_url | STRING | TEXT  |
| id | NUMBER | NUMBER  |
| number | INTEGER | INTEGER  |
| title | STRING | TEXT  |
| state | STRING | TEXT  |
| assignees | [{STRING\(login), STRING\(id), STRING\(html_url), STRING\(type)}] | ARRAY_BUILDER  |
| labels | [{STRING\(id), STRING\(name), STRING\(description)}] | ARRAY_BUILDER  |
| body | STRING | TEXT  |






### Get Issue
Name: getIssue

Get information from a specific issue

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  | true  |
| issue | Issue | STRING | SELECT  |  The issue you want to get details from.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| repository_url | STRING | TEXT  |
| id | NUMBER | NUMBER  |
| number | INTEGER | INTEGER  |
| title | STRING | TEXT  |
| state | STRING | TEXT  |
| assignees | [{STRING\(login), STRING\(id), STRING\(html_url), STRING\(type)}] | ARRAY_BUILDER  |
| labels | [{STRING\(id), STRING\(name), STRING\(description)}] | ARRAY_BUILDER  |
| body | STRING | TEXT  |






### List Issues
Name: listIssues

Retrieve issues assigned to the authenticated user across all accessible repositories.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| filter | Filter | STRING | SELECT  |  Specifies the types of issues to return.  |  true  |
| state | State | STRING | SELECT  |  Indicates the state of the issues to return.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(url), STRING\(repository_url), NUMBER\(id), INTEGER\(number), STRING\(title), STRING\(state), [{STRING\(login), STRING\(id), STRING\(html_url), STRING\(type)}]\(assignees), [{STRING\(id), STRING\(name), STRING\(description)}]\(labels), STRING\(body)} | OBJECT_BUILDER  |






### List Repository Issues
Name: listRepositoryIssues

Lists issues in a repository. Only open issues will be listed.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  The name of the repository  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(url), STRING\(repository_url), NUMBER\(id), INTEGER\(number), STRING\(title), STRING\(state), [{STRING\(login), STRING\(id), STRING\(html_url), STRING\(type)}]\(assignees), [{STRING\(id), STRING\(name), STRING\(description)}]\(labels), STRING\(body)} | OBJECT_BUILDER  |






### Star Repository
Name: starRepository

Stars a repository for the authenticated user.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| owner | Owner | STRING | TEXT  |  The account owner of the repository. The name is not case sensitive.  |  true  |
| repository | Repository | STRING | TEXT  |  The name of the repository including owner without the .git extension. The name is not case sensitive.  |  true  |






## Triggers


### New Issue
Triggers when a new issue is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| issue | {STRING\(url), STRING\(repository_url), NUMBER\(id), INTEGER\(number), STRING\(title), STRING\(state), STRING\(body)} | OBJECT_BUILDER  |
| sender | {STRING\(login), INTEGER\(id)} | OBJECT_BUILDER  |
| action | STRING | TEXT  |
| starred_at | STRING | TEXT  |
| repository | {INTEGER\(id), STRING\(name), STRING\(full_name), {STRING\(login), INTEGER\(id)}\(owner), STRING\(visibility), INTEGER\(forks), INTEGER\(open_issues), STRING\(default_branch)} | OBJECT_BUILDER  |







### New Pull Request
Triggers when a new pull request is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| repository | Repository | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| number | INTEGER | INTEGER  |
| pull_request | {INTEGER\(id), STRING\(state), STRING\(title), STRING\(body), INTEGER\(commits)} | OBJECT_BUILDER  |
| sender | {STRING\(login), INTEGER\(id)} | OBJECT_BUILDER  |
| action | STRING | TEXT  |
| repository | {INTEGER\(id), STRING\(name), STRING\(full_name), {STRING\(login), INTEGER\(id)}\(owner), STRING\(visibility), INTEGER\(forks), INTEGER\(open_issues), STRING\(default_branch)} | OBJECT_BUILDER  |







<hr />

<hr />

# Additional instructions
<hr />

## Connection Setup

1. Login to your GitHub account.
2. Click on your profile icon in the top right corner.
3. Select **Settings** from the dropdown menu.
4. In the left sidebar, click on **Developer settings**.
5. In the left sidebar, click on **OAuth Apps**.
6. Click on **New OAuth App**.
7. Fill in the required fields:
    - **Application name**: Enter a name for your application (e.g., `Test App`).
    - **Homepage URL**: Provide the homepage URL for your application (e.g., `https://www.bytechef.io/`).
    - **Authorization callback URL**: Specify the URL where users will be redirected after authorization (e.g., `http://127.0.0.1:5173/callback`).
8. Click **Register application**.
9. Click on **Generate a new client secret**.
10. Copy the **Client ID** and **Client Secret** for later use.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)">
<iframe src="https://www.guidejar.com/embed/bhsAUb5TGIexsFuLBica?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>
