---
title: "Box"
description: "Box is a cloud content management and file sharing service that enables businesses to securely store, manage, and collaborate on documents."
---
## Reference
<hr />

Box is a cloud content management and file sharing service that enables businesses to securely store, manage, and collaborate on documents.

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


### Create folder
Creates a new empty folder within the specified parent folder.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Folder name | STRING | TEXT  |
| Parent folder | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Download file
Download a selected file.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Parent folder | STRING | SELECT  |
| File | STRING | SELECT  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Upload file
Uploads a small file to Box.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Parent folder | STRING | SELECT  |
| File | FILE_ENTRY | FILE_ENTRY  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ARRAY | ARRAY_BUILDER  |





