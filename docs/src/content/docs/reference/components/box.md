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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers


### New File
Triggers when file is uploaded to folder.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Folder | STRING | SELECT  |  Folder in which file uploads will trigger this webhook.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(type), STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







### New Folder
Triggers when folder is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Folder | STRING | SELECT  |  Folder in which new folder will trigger this webhook.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(type), STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







<hr />



## Actions


### Create folder
Creates a new empty folder within the specified parent folder.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Folder name | STRING | TEXT  |  The name for the new folder.  |
| Parent folder | STRING | SELECT  |  Folder where the new folder will be created; if no folder is selected, the folder will be created in the root folder.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(type), STRING\(id), STRING\(name)} | OBJECT_BUILDER  |






### Download file
Download a selected file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Parent folder | STRING | SELECT  |  Folder from which you want to download the file.  |
| File | STRING | SELECT  |  File to download.  |


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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Parent folder | STRING | SELECT  |  Folder where the file should be uploaded; if no folder is selected, the file will be uploaded in the root folder.  |
| File | FILE_ENTRY | FILE_ENTRY  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{STRING\(type), STRING\(id), STRING\(name), {STRING\(type), STRING\(id), STRING\(name)}\(parent)}] | ARRAY_BUILDER  |






<hr />

# Additional instructions
<hr />

![anl-c-box-md](https://static.scarf.sh/a.png?x-pxid=84d37904-17b6-42f2-ae30-b656ae3c7561)
## CONNECTION

[Setting up OAuth2](https://developer.box.com/guides/authentication/oauth2/oauth2-setup/)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.85379203% + 32px)"><iframe src="https://www.guidejar.com/embed/e7edcd34-573c-4ccc-af27-4040237a49b9?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
