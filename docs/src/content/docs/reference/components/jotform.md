---
title: "JotForm"
description: "JotForm is an online form builder that enables users to create customized forms for various purposes without needing coding skills."
---

JotForm is an online form builder that enables users to create customized forms for various purposes without needing coding skills.


Categories: surveys-and-feedback


Type: jotform/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| region | Region | STRING | SELECT  |  | true  |
| key | Key | STRING | TEXT  |  | true  |
| value | API Key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Get Form Submissions
Get all submissions for a specific form.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| formId | Form ID | STRING | SELECT  |  ID of the form to retrieve submissions for.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{STRING\(id), STRING\(form_id), STRING\(status), STRING\(new), STRING\(flag), STRING\(notes)}]\(content)} | OBJECT_BUILDER  |








## Triggers



<hr />

