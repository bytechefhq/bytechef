---
title: "Figma"
description: "Figma is a cloud-based design and prototyping tool that enables teams to collaborate in real-time on user interface and user experience projects."
---

Figma is a cloud-based design and prototyping tool that enables teams to collaborate in real-time on user interface and user experience projects.


Categories: productivity-and-collaboration


Type: figma/v1

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


### Get Comments
Gets a list of comments left on the file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileKey | File Key | STRING | TEXT  |  File to get comments from. Figma file key copy from Figma file URL.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | [{STRING\(id), STRING\(file_key), STRING\(parent_id), {STRING\(id), STRING\(handle), STRING\(img_url), STRING\(email)}\(user)}] | ARRAY_BUILDER  |






### Post Comment
Posts a new comment on the file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileKey | File Key | STRING | TEXT  |  File to add comments in. Figma file key copy from Figma file URL.  |  true  |
| __item | Item | {STRING\(message)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(file_key), STRING\(parent_id), STRING\(message)} | OBJECT_BUILDER  |








## Triggers



<hr />

