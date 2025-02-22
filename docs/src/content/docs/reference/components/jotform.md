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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| region | Region | STRING <details> <summary> Options </summary> us, eu </details> |  | true |
| key | Key | STRING |  | true |
| value | API Key | STRING |  | true |





<hr />



## Actions


### Get Form Submissions
Name: getFormSubmissions

Get all submissions for a specific form.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| formId | Form ID | STRING | ID of the form to retrieve submissions for. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{STRING\(id), STRING\(form_id), STRING\(status), STRING\(new), STRING\(flag), STRING\(notes)}]\(content)} </details> |




#### JSON Example
```json
{
  "label" : "Get Form Submissions",
  "name" : "getFormSubmissions",
  "parameters" : {
    "formId" : ""
  },
  "type" : "jotform/v1/getFormSubmissions"
}
```




