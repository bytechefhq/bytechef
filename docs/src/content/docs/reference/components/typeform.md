---
title: "Typeform"
description: "Typeform is an online survey and form-building tool that enables users to create interactive and engaging forms for collecting data and feedback."
---

Typeform is an online survey and form-building tool that enables users to create interactive and engaging forms for collecting data and feedback.


Categories: surveys-and-feedback


Type: typeform/v1

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


### Create Form
Creates a new form

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Title | STRING | TEXT  |  Title to use for the form.  |  true  |
| type | Type | STRING | SELECT  |  Form type for the typeform.  |  false  |
| workspace | Workspace | STRING | SELECT  |  Workspace where the form will be created.  |  false  |


#### Output



Type: OBJECT









## Triggers


### New Submission
Triggers when form is submitted.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| form | Form Name | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT








<hr />

