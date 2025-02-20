---
title: "Box"
description: "Box is a cloud content management and file sharing service that enables businesses to securely store, manage, and collaborate on documents."
---

Box is a cloud content management and file sharing service that enables businesses to securely store, manage, and collaborate on documents.


Categories: file-storage


Type: box/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Folder
Name: createFolder

Creates a new empty folder within the specified parent folder.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Folder Name | STRING | TEXT | The name for the new folder. | true |
| id | Parent Folder ID | STRING | SELECT | ID of the folder where the new folder will be created; if no folder is selected, the folder will be created in the root folder. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| type | STRING | TEXT |
| id | STRING | TEXT |
| name | STRING | TEXT |
| parent | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Folder",
  "name" : "createFolder",
  "parameters" : {
    "name" : "",
    "id" : ""
  },
  "type" : "box/v1/createFolder"
}
```


### Download File
Name: downloadFile

Download a selected file.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Parent Folder ID | STRING | SELECT | ID of the folder from which you want to download the file. | true |
| fileId | File ID | STRING <details> <summary> Depends On </summary> id </details> | SELECT | ID of the file to download. | true |


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
    "id" : "",
    "fileId" : ""
  },
  "type" : "box/v1/downloadFile"
}
```


### Upload File
Name: uploadFile

Uploads a small file to Box.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Parent Folder ID | STRING | SELECT | ID of the folder where the file should be uploaded; if no folder is selected, the file will be uploaded in the root folder. | true |
| file | File | FILE_ENTRY | FILE_ENTRY |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| entries | ARRAY <details> <summary> Items </summary> [{STRING\(type), STRING\(id), STRING\(name), {STRING\(type), STRING\(id), STRING\(name)}\(parent)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Upload File",
  "name" : "uploadFile",
  "parameters" : {
    "id" : "",
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "box/v1/uploadFile"
}
```




## Triggers


### New File
Name: newFile

Triggers when file is uploaded to folder.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| folderId | Folder ID | STRING | SELECT | ID of the folder in which file uploads will trigger this webhook. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| type | STRING | TEXT |
| id | STRING | TEXT |
| name | STRING | TEXT |
| parent | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "New File",
  "name" : "newFile",
  "parameters" : {
    "folderId" : ""
  },
  "type" : "box/v1/newFile"
}
```


### New Folder
Name: newFolder

Triggers when folder is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| folderId | Folder ID | STRING | SELECT | ID of the folder in which new folder will trigger this webhook. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| type | STRING | TEXT |
| id | STRING | TEXT |
| name | STRING | TEXT |
| parent | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "New Folder",
  "name" : "newFolder",
  "parameters" : {
    "folderId" : ""
  },
  "type" : "box/v1/newFolder"
}
```


<hr />

<hr />

# Additional instructions
<hr />

![anl-c-box-md](https://static.scarf.sh/a.png?x-pxid=84d37904-17b6-42f2-ae30-b656ae3c7561)
## CONNECTION

[Setting up OAuth2](https://developer.box.com/guides/authentication/oauth2/oauth2-setup/)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.85379203% + 32px)"><iframe src="https://www.guidejar.com/embed/e7edcd34-573c-4ccc-af27-4040237a49b9?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
