---
title: "Github"
description: "Developer platform that allows developers to create, store, manage and share their code"
---
## Reference
<hr />

Developer platform that allows developers to create, store, manage and share their code

Categories: [DEVELOPER_TOOLS]

Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client id | STRING | TEXT  |
| Client secret | STRING | TEXT  |





<hr />





## Actions


### Create issue
Create a new issue

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Repository | STRING | SELECT  |
| Title | STRING | TEXT  |
| Body | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Get issue
Create a specific issue

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Repository | STRING | SELECT  |
| Issue | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Create Issue Comment
Create a comment for the issue

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Repository | STRING | SELECT  |
| Issue | STRING | SELECT  |
| Comment | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





