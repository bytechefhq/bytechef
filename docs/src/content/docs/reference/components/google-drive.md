---
title: "Google Drive"
description: "Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and collaborate online."
---
## Reference
<hr />

Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and collaborate online.


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

```{mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr, name=new-file.txt}```



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

```{mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr, name=new-file.txt}```



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

```{mimeType=plain/text, id=1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr, name=new-file.txt}```



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

[Guidejar](https://guidejar.com/guides/fec74020-26bb-43dd-814c-f8b907f6f45b) tutorial.

[Turning on Drive API](https://guidejar.com/guides/4e69ce7b-c430-443c-801c-b01ea2781c39)
