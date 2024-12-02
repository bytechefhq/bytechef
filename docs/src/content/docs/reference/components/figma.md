---
title: "Figma"
description: "Figma is a cloud-based design and prototyping tool that enables teams to collaborate in real-time on user interface and user experience projects."
---
## Reference
<hr />

Figma is a cloud-based design and prototyping tool that enables teams to collaborate in real-time on user interface and user experience projects.


Categories: [productivity-and-collaboration]


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



<hr />



## Actions


### Get Comments
Gets a list of comments left on the file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File Key | STRING | TEXT  |  File to get comments from. Figma file key copy from Figma file URL.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{STRING\(id), STRING\(file_key), STRING\(parent_id), {STRING\(id), STRING\(handle), STRING\(img_url), STRING\(email)}\(user)}] | ARRAY_BUILDER  |






### Post Comment
Posts a new comment on the file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File Key | STRING | TEXT  |  File to add comments in. Figma file key copy from Figma file URL.  |
| Item | {STRING\(message)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(file_key), STRING\(parent_id), STRING\(message)} | OBJECT_BUILDER  |






