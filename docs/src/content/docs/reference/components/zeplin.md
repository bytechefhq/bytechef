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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Update Project
Name: updateProject

Updates an existing project.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project_id | Project ID | STRING | Project to update. | true |
| name | Name | STRING | New name for the project. | true |
| description | Description | STRING | New description for the project. | false |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Update Project",
  "name" : "updateProject",
  "parameters" : {
    "project_id" : "",
    "name" : "",
    "description" : ""
  },
  "type" : "zeplin/v1/updateProject"
}
```




## Triggers


### Project Note
Name: projectNote

Triggers when new note is created, deleted or updated in specified project.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| project_id | Project ID | STRING | ID of the project you want to monitor. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| context | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(type), {STRING\(id), STRING\(status), [{STRING\(id), {STRING\(id), STRING\(email), STRING\(username)}\(author), STRING\(content)}]\(comments)}\(data)} </details> |
| action | STRING |
| event | STRING |




#### JSON Example
```json
{
  "label" : "Project Note",
  "name" : "projectNote",
  "parameters" : {
    "project_id" : ""
  },
  "type" : "zeplin/v1/projectNote"
}
```


<hr />

