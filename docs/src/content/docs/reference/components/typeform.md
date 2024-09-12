---
title: "Typeform"
description: "Typeform is an online survey and form-building tool that enables users to create interactive and engaging forms for collecting data and feedback."
---
## Reference
<hr />

Typeform is an online survey and form-building tool that enables users to create interactive and engaging forms for collecting data and feedback.


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



## Triggers


### New Submission
Triggers when form is submitted.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form name | STRING | SELECT  |  |


### Output



Type: OBJECT








<hr />



## Actions


### Create Form
Creates a new form

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Title | STRING | TEXT  |  Title to use for the form.  |
| Type | STRING | SELECT  |  Form type for the typeform.  |
| Workspace | STRING | SELECT  |  Workspace where the form will be created.  |


### Output



Type: OBJECT







