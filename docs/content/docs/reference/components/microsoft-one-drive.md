---
title: "Microsoft OneDrive"
description: "Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing files online."
---

Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing files online.


Categories: File Storage


Type: microsoftOneDrive/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |
| tenantId | Tenant Id | STRING |  | true |





<hr />



## Actions


### Download File
Name: downloadFile

Download a file from your Microsoft OneDrive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | ID of the folder from which you want to download the file. | false |
| id | File ID | STRING <details> <summary> Depends On </summary> parentId </details> | ID of the file to download. | true |

#### Example JSON Structure
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

#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| extension | STRING |  |
| mimeType | STRING |  |
| name | STRING |  |
| url | STRING |  |




#### Output Example
```json
{
  "extension" : "",
  "mimeType" : "",
  "name" : "",
  "url" : ""
}
```


### List Files
Name: listFiles

List files in a OneDrive folder.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | ID of the folder from which you want to list files. If no folder is specified, the root folder will be used. | false |

#### Example JSON Structure
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

#### Output



Type: ARRAY


Items Type: OBJECT


#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |





#### Output Example
```json
[ {
  "id" : "",
  "name" : ""
} ]
```


### List Folders
Name: listFolders

List folders in a OneDrive folder.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | ID of the Folder from which you want to list folders. If no folder is specified, the root folder will be used. | false |

#### Example JSON Structure
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

#### Output



Type: ARRAY


Items Type: OBJECT


#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |





#### Output Example
```json
[ {
  "id" : "",
  "name" : ""
} ]
```


### Upload File
Name: uploadFile

Upload a file to your Microsoft OneDrive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | ID of the Folder where the file should be uploaded; if no folder is selected, the file will be uploaded in the root folder. | false |
| file | File | FILE_ENTRY | File to upload. | true |

#### Example JSON Structure
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

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |




#### Output Example
```json
{
  "id" : "",
  "name" : ""
}
```




## Triggers


### New File
Name: newFile

Triggers when file is uploaded to folder.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| parentId | Parent Folder ID | STRING | If no folder is specified, the root folder will be used. | false |


#### Output



Type: ARRAY


Items Type: OBJECT


#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| name | STRING |  |





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

