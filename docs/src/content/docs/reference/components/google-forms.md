---
title: "Google Forms"
description: "Google Forms is a web-based application that allows users to create surveys, quizzes, and forms for data collection and analysis, with real-time collaboration and response tracking."
---
## Reference
<hr />

Google Forms is a web-based application that allows users to create surveys, quizzes, and forms for data collection and analysis, with real-time collaboration and response tracking.


Categories: [surveys-and-feedback]


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





## Actions


### Get Form
Get the information about a form.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form | STRING | SELECT  |  Form to retrieve.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| {STRING\(title), STRING\(documentTitle)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING\(itemdId), STRING\(title)}] | ARRAY_BUILDER  |






### Get Response
Get the response of a form.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form | STRING | SELECT  |  Form to retrieve.  |
| Response | STRING | SELECT  |  Response to retrieve.  |


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
| {} | OBJECT_BUILDER  |
| NUMBER | NUMBER  |






