---
title: "Zeplin"
description: "Zeplin is a collaboration tool that bridges the gap between designers and developers by providing a platform to share, organize, and translate design files into development."
---

Zeplin is a collaboration tool that bridges the gap between designers and developers by providing a platform to share, organize, and translate design files into development.


Categories: communication


Type: zeplin/v1

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


### Update Project
Updates an existing project.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project_id | Project ID | STRING | SELECT  |  Project to update.  |  true  |
| __item | Project | {STRING\(name), STRING\(description)} | OBJECT_BUILDER  |  | true  |






## Triggers


### Project Note
Triggers when new note is created, deleted or updated in specified project.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| project_id | Project ID | STRING | SELECT  |  ID of the project you want to monitor.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| context | {STRING\(id), STRING\(type), {STRING\(id), STRING\(status), [{STRING\(id), {STRING\(id), STRING\(email), STRING\(username)}\(author), STRING\(content)}]\(comments)}\(data)} | OBJECT_BUILDER  |
| action | STRING | TEXT  |
| event | STRING | TEXT  |







<hr />

