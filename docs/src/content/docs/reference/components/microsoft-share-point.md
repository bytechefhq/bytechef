---
title: "Microsoft SharePoint"
description: "Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations."
---

Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations.


Categories: file-storage, communication


Type: microsoftSharePoint/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| tenantId | Tenant Id | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Folder
Creates a new folder at path you specify.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteId | Site ID | STRING | SELECT  |  | true  |
| parentFolder | Parent Folder ID | STRING | SELECT  |  If no folder is selected, file will be uploaded to root folder.  |  false  |
| name | Folder Name | STRING | TEXT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |






### Create List
Creates a new list

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteId | Site ID | STRING | SELECT  |  | true  |
| displayName | List Name | STRING | TEXT  |  | true  |
| description | List Description | STRING | TEXT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| description | STRING | TEXT  |
| displayName | STRING | TEXT  |






### Create List Item
Creates a new item in a list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteId | Site ID | STRING | SELECT  |  | true  |
| listId | List ID | STRING | SELECT  |  | true  |
| columns | DYNAMIC_PROPERTIES | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |






### Upload File
Upload file to Microsoft SharePoint folder.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteId | Site ID | STRING | SELECT  |  | true  |
| parentFolder | Parent Folder ID | STRING | SELECT  |  If no folder is selected, file will be uploaded to root folder  |  false  |
| file | File | FILE_ENTRY | FILE_ENTRY  |  File to upload.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |








