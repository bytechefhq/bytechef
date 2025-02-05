---
title: "Microsoft OneDrive"
description: "Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing files online."
---

Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing files online.


Categories: file-storage


Type: microsoftOneDrive/v1

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


### Download File
Download a file from your Microsoft OneDrive.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| parentId | Parent Folder ID | STRING | SELECT  |  ID of the folder from which you want to download the file.  |  false  |
| id | File ID | STRING | SELECT  |  ID of the file to download.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### List Files
List files in a OneDrive folder.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| parentId | Parent Folder ID | STRING | SELECT  |  ID of the folder from which you want to list files. If no folder is specified, the root folder will be used.  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |






### List Folders
List folders in a OneDrive folder.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| parentId | Parent Folder ID | STRING | SELECT  |  ID of the Folder from which you want to list folders. If no folder is specified, the root folder will be used.  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |






### Upload File
Upload a file to your Microsoft OneDrive.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| parentId | Parent Folder ID | STRING | SELECT  |  ID of the Folder where the file should be uploaded; if no folder is selected, the file will be uploaded in the root folder.  |  false  |
| file | File | FILE_ENTRY | FILE_ENTRY  |  File to upload.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| name | STRING | TEXT  |








## Triggers


### New File
Triggers when file is uploaded to folder.

Type: POLLING
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| parentId | Parent Folder ID | STRING | SELECT  |  If no folder is specified, the root folder will be used.  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







<hr />

