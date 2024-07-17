---
title: "Microsoft OneDrive"
description: "Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing files online."
---
## Reference
<hr />

Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing files online.


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
| Tenant Id | STRING | TEXT  |  |





<hr />





## Actions


### Download file
Download a file from your Microsoft OneDrive

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Parent folder | STRING | SELECT  |  Folder from which you want to download the file.  |
| File | STRING | SELECT  |  File to download  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### List Files
List files in a OneDrive folder

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Parent folder | STRING | SELECT  |  Folder from which you want to list files. If no folder is specified, the root folder will be used.  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### List Folders
List folders in a OneDrive folder

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Parent folder | STRING | SELECT  |  Folder from which you want to list folders. If no folder is specified, the root folder will be used.  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Upload file
Upload a file to your Microsoft OneDrive

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Parent folder | STRING | SELECT  |  Folder where the file should be uploaded; if no folder is selected, the file will be uploaded in the root folder.  |
| File | FILE_ENTRY | FILE_ENTRY  |  File to upload  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |





