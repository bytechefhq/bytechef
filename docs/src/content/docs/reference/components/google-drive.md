---
title: "Google Drive"
description: "Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and collaborate online."
---
## Reference
<hr />

Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and collaborate online.


Categories: [file-storage]


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





<hr />





## Actions


### Upload file
Uploads a file in your Google Drive

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to upload.  |
| Parent folder | STRING | SELECT  |  Folder where the file will be uploaded; if no folder is selected, the file will be uploaded to the root folder.  |


### Output


___Sample Output:___

```{mimeType=plain/text, name=new-file.txt, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Read file
Read a selected file from Google Drive file.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | STRING | SELECT  |  The id of a file to read.  |


### Output



Type: FILE_ENTRY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create new folder
Creates a new empty folder in Google Drive.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Folder name | STRING | TEXT  |  The name of the new folder.  |
| Parent folder | STRING | SELECT  |  Folder where the new folder will be created; if no folder is selected, the folder will be created in the root folder.  |


### Output


___Sample Output:___

```{mimeType=plain/text, name=new-file.txt, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create new text file
Creates a new text file in Google Drive.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File name | STRING | TEXT  |  The name of the new text file.  |
| Text | STRING | TEXT_AREA  |  The text content to add to file.  |
| File type | STRING | SELECT  |  Select file type.  |
| Parent folder | STRING | SELECT  |  Folder where the file should be created; if no folder is selected, the file will be created in the root folder.  |


### Output


___Sample Output:___

```{mimeType=plain/text, name=new-file.txt, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr}```



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






<hr />

# Additional instructions
<hr />

![anl-c-google-drive-md](https://static.scarf.sh/a.png?x-pxid=8a7e290d-47ec-48ca-95f4-7fb515dc3b8a)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Drive API <div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/4e69ce7b-c430-443c-801c-b01ea2781c39?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
