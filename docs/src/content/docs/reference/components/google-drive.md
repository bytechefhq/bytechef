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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />





## Actions


### Upload file
Uploads a file in your Google Drive

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Parent folder | STRING | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | STRING | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Folder name | STRING | TEXT  |
| Parent folder | STRING | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File name | STRING | TEXT  |
| Text | STRING | TEXT_AREA  |
| File type | STRING | SELECT  |
| Parent folder | STRING | SELECT  |


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





