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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Copy File
Name: copyFile

Copy a selected file to a different location within Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | The id of the file to be copied. | true |
| fileName | New File Name | STRING | The name of the new file created as a result of the copy operation. | true |
| folderId | Destination Folder ID | STRING | The ID of the folder where the copied file will be stored. | true |

#### Example JSON Structure
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

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |




#### Output Example
```json
{
  "id" : "",
  "kind" : "",
  "mimeType" : "",
  "name" : ""
}
```


### Create New Folder
Name: createNewFolder

Creates a new empty folder in Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| folderName | Folder Name | STRING | The name of the new folder. | true |
| folderId | Parent Folder ID | STRING | ID of the folder where the new folder will be created; if no folder is selected, the folder will be created in the root folder. | false |

#### Example JSON Structure
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

#### Output


___Sample Output:___

```{name=new-file.txt, mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |




#### Output Example
```json
{
  "id" : "",
  "kind" : "",
  "mimeType" : "",
  "name" : ""
}
```


### Create New Text File
Name: createNewTextFile

Creates a new text file in Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileName | File Name | STRING | The name of the new text file. | true |
| text | Text | STRING | The text content to add to file. | true |
| mimeType | File Type | STRING <details> <summary> Options </summary> plain/text, text/csv, text/xml </details> | Select file type. | true |
| folderId | Parent Folder ID | STRING | ID of the folder where the file should be created; if no folder is selected, the file will be created in the root folder. | false |

#### Example JSON Structure
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

#### Output


___Sample Output:___

```{name=new-file.txt, mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |




#### Output Example
```json
{
  "id" : "",
  "kind" : "",
  "mimeType" : "",
  "name" : ""
}
```


### Delete File
Name: deleteFile

Delete a selected file from Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | The id of a file to delete. | true |

#### Example JSON Structure
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

#### Output

This action does not produce any output.




### Download File
Name: downloadFile

Download selected file from Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | ID of the file to download. | true |

#### Example JSON Structure
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


### Get File
Name: getFile

Retrieve a specified file from your Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileId | File ID | STRING | ID of the file to be retrieved. | true |

#### Example JSON Structure
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

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |




#### Output Example
```json
{
  "id" : "",
  "kind" : "",
  "mimeType" : "",
  "name" : ""
}
```


### Upload File
Name: uploadFile

Uploads a file in your Google Drive.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | The object property which contains a reference to the file to upload. | true |
| folderId | Parent Folder ID | STRING | ID of the folder where the file will be uploaded; if no folder is selected, the file will be uploaded to the root folder. | false |

#### Example JSON Structure
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

#### Output


___Sample Output:___

```{name=new-file.txt, mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |




#### Output Example
```json
{
  "id" : "",
  "kind" : "",
  "mimeType" : "",
  "name" : ""
}
```




## Triggers


### New File
Name: newFile

Triggers when new file is uploaded to Google Drive.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| folderId | Parent Folder | STRING |  | true |


#### Output



Type: ARRAY


Items Type: OBJECT


#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |





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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| folderId | Parent Folder | STRING |  | true |


#### Output



Type: ARRAY


Items Type: OBJECT


#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the file. |
| kind | STRING | Identifies what kind of resource this is. |
| mimeType | STRING | The MIME type of the file. |
| name | STRING | The name of the file. |





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
