---
title: "Google Drive"
description: "Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and collaborate online."
---

Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and collaborate online.


Categories: file-storage


Type: googleDrive/v1

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


### Copy File
Name: copyFile

Copy a selected file to a different location within Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | SELECT | The id of the file to be copied. | true |
| fileName | New File Name | STRING | TEXT | The name of the new file created as a result of the copy operation. | true |
| folderId | Destination Folder ID | STRING | SELECT | The ID of the folder where the copied file will be stored. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| kind | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Copy File",
  "name" : "copyFile",
  "parameters" : {
    "fileId" : "",
    "fileName" : "",
    "folderId" : ""
  },
  "type" : "googleDrive/v1/copyFile"
}
```


### Create New Folder
Name: createNewFolder

Creates a new empty folder in Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| folderName | Folder Name | STRING | TEXT | The name of the new folder. | true |
| folderId | Parent Folder ID | STRING | SELECT | ID of the folder where the new folder will be created; if no folder is selected, the folder will be created in the root folder. | false |


#### Output


___Sample Output:___

```{name=new-file.txt, mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create New Folder",
  "name" : "createNewFolder",
  "parameters" : {
    "folderName" : "",
    "folderId" : ""
  },
  "type" : "googleDrive/v1/createNewFolder"
}
```


### Create New Text File
Name: createNewTextFile

Creates a new text file in Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileName | File Name | STRING | TEXT | The name of the new text file. | true |
| text | Text | STRING | TEXT_AREA | The text content to add to file. | true |
| mimeType | File Type | STRING <details> <summary> Options </summary> plain/text, text/csv, text/xml </details> | SELECT | Select file type. | true |
| folderId | Parent Folder ID | STRING | SELECT | ID of the folder where the file should be created; if no folder is selected, the file will be created in the root folder. | false |


#### Output


___Sample Output:___

```{name=new-file.txt, mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create New Text File",
  "name" : "createNewTextFile",
  "parameters" : {
    "fileName" : "",
    "text" : "",
    "mimeType" : "",
    "folderId" : ""
  },
  "type" : "googleDrive/v1/createNewTextFile"
}
```


### Delete File
Name: deleteFile

Delete a selected file from Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | SELECT | The id of a file to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete File",
  "name" : "deleteFile",
  "parameters" : {
    "fileId" : ""
  },
  "type" : "googleDrive/v1/deleteFile"
}
```


### Download File
Name: downloadFile

Download selected file from Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | SELECT | ID of the file to download. | true |


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
    "fileId" : ""
  },
  "type" : "googleDrive/v1/downloadFile"
}
```


### Get File
Name: getFile

Retrieve a specified file from your Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | SELECT | ID of the file to be retrieved. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| kind | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Get File",
  "name" : "getFile",
  "parameters" : {
    "fileId" : ""
  },
  "type" : "googleDrive/v1/getFile"
}
```


### Upload File
Name: uploadFile

Uploads a file in your Google Drive.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY | The object property which contains a reference to the file to upload. | true |
| folderId | Parent Folder ID | STRING | SELECT | ID of the folder where the file will be uploaded; if no folder is selected, the file will be uploaded to the root folder. | false |


#### Output


___Sample Output:___

```{name=new-file.txt, mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| mimeType | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Upload File",
  "name" : "uploadFile",
  "parameters" : {
    "fileEntry" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    },
    "folderId" : ""
  },
  "type" : "googleDrive/v1/uploadFile"
}
```




## Triggers


### New File
Name: newFile

Triggers when new file is uploaded to Google Drive.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| folderId | Parent Folder | STRING | SELECT |  | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(mimeType), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "New File",
  "name" : "newFile",
  "parameters" : {
    "folderId" : ""
  },
  "type" : "googleDrive/v1/newFile"
}
```


### New Folder
Name: newFolder

Triggers when new folder is uploaded to Google Drive.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| folderId | Parent Folder | STRING | SELECT |  | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(mimeType), STRING\(name)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "New Folder",
  "name" : "newFolder",
  "parameters" : {
    "folderId" : ""
  },
  "type" : "googleDrive/v1/newFolder"
}
```


<hr />

<hr />

# Additional instructions
<hr />

![anl-c-google-drive-md](https://static.scarf.sh/a.png?x-pxid=8a7e290d-47ec-48ca-95f4-7fb515dc3b8a)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Drive API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/4e69ce7b-c430-443c-801c-b01ea2781c39?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
