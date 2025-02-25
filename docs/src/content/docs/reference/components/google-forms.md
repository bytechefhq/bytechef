---
title: "Google Forms"
description: "Google Forms is a web-based application that allows users to create surveys, quizzes, and forms for data collection and analysis, with real-time collaboration and response tracking."
---

Google Forms is a web-based application that allows users to create surveys, quizzes, and forms for data collection and analysis, with real-time collaboration and response tracking.


Categories: surveys-and-feedback


Type: googleForms/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Get All Responses
Name: getAllResponses

Get all responses of a form.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| formId | Form ID | STRING | ID of the form whose responses to retrieve. | true |


#### JSON Example
```json
{
  "label" : "Get All Responses",
  "name" : "getAllResponses",
  "parameters" : {
    "formId" : ""
  },
  "type" : "googleForms/v1/getAllResponses"
}
```


### Get Form
Name: getForm

Get the information about a form.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| formId | Form ID | STRING | ID of the form to retrieve. | true |


#### JSON Example
```json
{
  "label" : "Get Form",
  "name" : "getForm",
  "parameters" : {
    "formId" : ""
  },
  "type" : "googleForms/v1/getForm"
}
```


### Get Response
Name: getResponse

Get the response of a form.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| formId | Form ID | STRING | ID of the form whose response to retrieve. | true |
| responseId | Response ID | STRING <details> <summary> Depends On </summary> formId </details> | ID of the response to retrieve. | true |


#### JSON Example
```json
{
  "label" : "Get Response",
  "name" : "getResponse",
  "parameters" : {
    "formId" : "",
    "responseId" : ""
  },
  "type" : "googleForms/v1/getResponse"
}
```




## Triggers


### New Response
Name: newResponse

Triggers when response is submitted to Google Form.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| formId | Form | STRING | Form to watch for new responses. | true |


#### JSON Example
```json
{
  "label" : "New Response",
  "name" : "newResponse",
  "parameters" : {
    "formId" : ""
  },
  "type" : "googleForms/v1/newResponse"
}
```


<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Forms API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/6O0wffw3j1b6d9hcAxOy?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
