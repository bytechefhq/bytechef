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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Update Project
Name: updateProject

Updates an existing project.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| project_id | Project ID | STRING | SELECT | Project to update. | true |
| __item | Project | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(description)} </details> | OBJECT_BUILDER |  | true |


#### JSON Example
```json
{
  "label" : "Update Project",
  "name" : "updateProject",
  "parameters" : {
    "project_id" : "",
    "__item" : {
      "name" : "",
      "description" : ""
    }
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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| project_id | Project ID | STRING | SELECT | ID of the project you want to monitor. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| context | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(type), {STRING\(id), STRING\(status), [{STRING\(id), {STRING\(id), STRING\(email), STRING\(username)}\(author), STRING\(content)}]\(comments)}\(data)} </details> | OBJECT_BUILDER |
| action | STRING | TEXT |
| event | STRING | TEXT |




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

