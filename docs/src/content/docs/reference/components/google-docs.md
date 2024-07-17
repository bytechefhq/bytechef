---
title: "Google Docs"
description: "Google Docs is a cloud-based collaborative word processing platform that allows multiple users to create, edit, and share documents in real-time."
---
## Reference
<hr />

Google Docs is a cloud-based collaborative word processing platform that allows multiple users to create, edit, and share documents in real-time.


Categories: [FILE_STORAGE]


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


### Create document
Create a document on Google Docs

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Title | STRING | TEXT  |  Document title  |
| Content | STRING | TEXT  |  Document content  |




### Edit template file
Edit a template file and replace the values with the ones provided

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Destination file | STRING | TEXT  |  The ID of the file to replace the values  |
| Variables | {} | OBJECT_BUILDER  |  Don't include the "[[]]", only the key name and its value  |
| Images | {} | OBJECT_BUILDER  |  Key: Image ID (get it manually from the Read File Action), Value: Image URL  |




### Read document
Read a document from Google Docs

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Document id | STRING | TEXT  |  The ID of the document to read  |




<hr />

# Additional instructions
<hr />

![anl-c-google-docs-md](https://static.scarf.sh/a.png?x-pxid=44cee406-a4a2-4c9f-80f5-bd560babff6e)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

[Guidejar](https://guidejar.com/guides/fec74020-26bb-43dd-814c-f8b907f6f45b) tutorial.

[Turning on Docs API](https://guidejar.com/guides/2fbfa39b-38f6-43f4-a55d-6f8d0588f6fb)
