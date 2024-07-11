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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />





## Actions


### Create document
Create a document on Google Docs

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Title | STRING | TEXT  |
| Content | STRING | TEXT  |




### Edit template file
Edit a template file and replace the values with the ones provided

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Destination file | STRING | TEXT  |
| Variables | OBJECT | OBJECT_BUILDER  |
| Images | OBJECT | OBJECT_BUILDER  |




### Read document
Read a document from Google Docs

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Document id | STRING | TEXT  |




