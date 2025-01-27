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



## Triggers


### New Response
Triggers when response is submitted to Google Form.

#### Type: POLLING
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form | STRING | SELECT  |  Form to watch for new responses.  |





<hr />



## Actions


### Get All Responses
Get all responses of a form.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form ID | STRING | SELECT  |  ID of the form whose responses to retrieve.  |




### Get Form
Get the information about a form.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form ID | STRING | SELECT  |  ID of the form to retrieve.  |




### Get Response
Get the response of a form.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Form ID | STRING | SELECT  |  ID of the form whose response to retrieve.  |
| Response ID | STRING | SELECT  |  ID of the response to retrieve.  |




<hr />

# Additional instructions
<hr />

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Forms API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/6O0wffw3j1b6d9hcAxOy?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
