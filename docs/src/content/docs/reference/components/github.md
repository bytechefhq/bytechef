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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client id | STRING | TEXT  |  |
| Client secret | STRING | TEXT  |  |





<hr />





## Actions


### Create issue
Create a new issue

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  Select an repository  |
| Title | STRING | TEXT  |  Title of the issue  |
| Body | STRING | TEXT  |  The contents of the issue  |


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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  Select an repository  |
| Issue | STRING | SELECT  |  Select a issue  |


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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Repository | STRING | SELECT  |  Select an repository  |
| Issue | STRING | SELECT  |  Select a issue  |
| Comment | STRING | TEXT  |  Create a issue comment  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





