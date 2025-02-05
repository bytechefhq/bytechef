---
title: "Google Slides"
description: "Google Slides is a cloud-based presentation software that allows users to create, edit, and collaborate on presentations online in real-time."
---

Google Slides is a cloud-based presentation software that allows users to create, edit, and collaborate on presentations online in real-time.


Categories: file-storage


Type: googleSlides/v1

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


### Create Presentation Based on Template
Creates a new presentation based on an existing one and can replace any placeholder variables found in your template presentation, like [[name]], [[email]], etc.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileId | Template Presentation ID | STRING | SELECT  |  The ID of the template presentation from which the new presentation will be created.  |  true  |
| fileName | New Presentation Name | STRING | TEXT  |  Name of the new presentation.  |  true  |
| folderId | Folder ID | STRING | SELECT  |  ID of the folder where the new presentation will be saved. If not provided, the new presentation will be saved in the same folder as the template presentation.  |  false  |
| values | Values | {} | OBJECT_BUILDER  |  Don't include the "[[]]", only the key name and its value.  |  true  |






<hr />

# Additional instructions
<hr />

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Slides API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/oO5B0kqdh0w4eOIIEz21?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
