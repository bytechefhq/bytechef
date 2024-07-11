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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |
| Tenant Id | STRING | TEXT  |





<hr />





## Actions


### Download file
Download a file from your Microsoft OneDrive

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Parent folder | STRING | SELECT  |
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





### List Files
List files in a OneDrive folder

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Parent folder | STRING | SELECT  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### List Folders
List folders in a OneDrive folder

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Parent folder | STRING | SELECT  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Upload file
Upload a file to your Microsoft OneDrive

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Parent folder | STRING | SELECT  |
| File | FILE_ENTRY | FILE_ENTRY  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |





