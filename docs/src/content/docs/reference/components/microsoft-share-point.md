---
title: "Microsoft SharePoint"
description: "Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations."
---
## Reference
<hr />

Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations.


Categories: [FILE_STORAGE, COMMUNICATION]


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


### Create folder
Creates a new folder at path you specify.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site | STRING | SELECT  |  |
| Parent folder | STRING | SELECT  |  If no folder is selected, file will be uploaded to root folder.  |
| Folder name | STRING | TEXT  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |





### Create list
Creates a new list

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site | STRING | SELECT  |  |
| List name | STRING | TEXT  |  |
| List description | STRING | TEXT  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Create list item
Creates a new item in a list.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site | STRING | SELECT  |  |
| List | STRING | SELECT  |  |
| DYNAMIC_PROPERTIES | null  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |





### Upload file
Upload file to Microsoft SharePoint folder.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site | STRING | SELECT  |  |
| Parent folder | STRING | SELECT  |  If no folder is selected, file will be uploaded to root folder  |
| File | FILE_ENTRY | FILE_ENTRY  |  File to upload.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |





