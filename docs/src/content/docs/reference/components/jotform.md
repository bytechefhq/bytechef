---
title: "JotForm"
description: "JotForm is an online form builder that enables users to create customized forms for various purposes without needing coding skills."
---
## Reference
<hr />

JotForm is an online form builder that enables users to create customized forms for various purposes without needing coding skills.


Categories: [surveys-and-feedback]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Region | STRING | SELECT  |  |
| Key | STRING | TEXT  |  |
| API Key | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Get Form Submissions
Get all submissions for a specific form.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form ID | STRING | SELECT  |  ID of the form to retrieve submissions for.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{STRING\(id), STRING\(form_id), STRING\(status), STRING\(new), STRING\(flag), STRING\(notes)}]\(content)} | OBJECT_BUILDER  |






