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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| tenantId | Tenant Id | STRING | TEXT |  | true |





<hr />



## Actions


### Download File
Name: downloadFile

Download a file from your Microsoft OneDrive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | SELECT | ID of the folder from which you want to download the file. | false |
| id | File ID | STRING <details> <summary> Depends On </summary> parentId </details> | SELECT | ID of the file to download. | true |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| extension | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |
| url | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Download File",
  "name" : "downloadFile",
  "parameters" : {
    "parentId" : "",
    "id" : ""
  },
  "type" : "microsoftOneDrive/v1/downloadFile"
}
```


### List Files
Name: listFiles

List files in a OneDrive folder.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | SELECT | ID of the folder from which you want to list files. If no folder is specified, the root folder will be used. | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "List Files",
  "name" : "listFiles",
  "parameters" : {
    "parentId" : ""
  },
  "type" : "microsoftOneDrive/v1/listFiles"
}
```


### List Folders
Name: listFolders

List folders in a OneDrive folder.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | SELECT | ID of the Folder from which you want to list folders. If no folder is specified, the root folder will be used. | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "List Folders",
  "name" : "listFolders",
  "parameters" : {
    "parentId" : ""
  },
  "type" : "microsoftOneDrive/v1/listFolders"
}
```


### Upload File
Name: uploadFile

Upload a file to your Microsoft OneDrive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | SELECT | ID of the Folder where the file should be uploaded; if no folder is selected, the file will be uploaded in the root folder. | false |
| file | File | FILE_ENTRY | FILE_ENTRY | File to upload. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Upload File",
  "name" : "uploadFile",
  "parameters" : {
    "parentId" : "",
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "microsoftOneDrive/v1/uploadFile"
}
```




## Triggers


### New File
Name: newFile

Triggers when file is uploaded to folder.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | SELECT | If no folder is specified, the root folder will be used. | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "New File",
  "name" : "newFile",
  "parameters" : {
    "parentId" : ""
  },
  "type" : "microsoftOneDrive/v1/newFile"
}
```


<hr />

