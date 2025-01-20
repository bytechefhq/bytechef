---
title: "Google Docs"
description: "Google Docs is a cloud-based collaborative word processing platform that allows multiple users to create, edit, and share documents in real-time."
---
## Reference
<hr />

Google Docs is a cloud-based collaborative word processing platform that allows multiple users to create, edit, and share documents in real-time.


Categories: [file-storage]


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


### Create Document
Create a document on Google Docs.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Title | STRING | TEXT  |  Document title.  |
| Content | STRING | TEXT_AREA  |  Document content.  |




### Edit Template File
Edit a template file and replace the values with the ones provided.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Template File ID | STRING | SELECT  |  The ID of the file to replace the values.  |
| Variables | {} | OBJECT_BUILDER  |  Don't include the "[[]]", only the key name and its value.  |
| Images | {} | OBJECT_BUILDER  |  Key: Image ID (get it manually from the Read File Action), Value: Image URL.  |




### Get Document
Retrieve a specified document from your Google Drive.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Document Id | STRING | SELECT  |  The ID of the document to read.  |




<hr />

# Additional instructions
<hr />

![anl-c-google-docs-md](https://static.scarf.sh/a.png?x-pxid=44cee406-a4a2-4c9f-80f5-bd560babff6e)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Docs API <div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/2fbfa39b-38f6-43f4-a55d-6f8d0588f6fb?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
