---
title: "Microsoft SharePoint"
description: "Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations."
---
## Reference
<hr />

Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations.


Categories: [file-storage, communication]


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
| Tenant Id | STRING | TEXT  |  |





<hr />





## Actions


### Create Folder
Creates a new folder at path you specify.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site ID | STRING | SELECT  |  |
| Parent Folder ID | STRING | SELECT  |  If no folder is selected, file will be uploaded to root folder.  |
| Folder Name | STRING | TEXT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |






### Create List
Creates a new list

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site ID | STRING | SELECT  |  |
| List Name | STRING | TEXT  |  |
| List Description | STRING | TEXT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create List Item
Creates a new item in a list.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site ID | STRING | SELECT  |  |
| List ID | STRING | SELECT  |  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |






### Upload File
Upload file to Microsoft SharePoint folder.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site ID | STRING | SELECT  |  |
| Parent Folder ID | STRING | SELECT  |  If no folder is selected, file will be uploaded to root folder  |
| File | FILE_ENTRY | FILE_ENTRY  |  File to upload.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |






