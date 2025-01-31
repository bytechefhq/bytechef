---
title: "Zeplin"
description: "Zeplin is a collaboration tool that bridges the gap between designers and developers by providing a platform to share, organize, and translate design files into development."
---
## Reference
<hr />

Zeplin is a collaboration tool that bridges the gap between designers and developers by providing a platform to share, organize, and translate design files into development.


Categories: [communication]


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


### Project Note
Triggers when new note is created, deleted or updated in specified project.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project ID | STRING | SELECT  |  ID of the project you want to monitor.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(type), {STRING\(id), STRING\(status), [{STRING\(id), {STRING\(id), STRING\(email), STRING\(username)}\(author), STRING\(content)}]\(comments)}\(data)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |







<hr />



## Actions


### Update Project
Updates an existing project.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Project ID | STRING | SELECT  |  Project to update.  |
| Project | {STRING\(name), STRING\(description)} | OBJECT_BUILDER  |  |




