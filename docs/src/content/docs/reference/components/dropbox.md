---
title: "Dropbox"
description: "Dropbox is a file hosting service that offers cloud storage, file synchronization, personal cloud, and client software."
---
## Reference
<hr />

Dropbox is a file hosting service that offers cloud storage, file synchronization, personal cloud, and client software.

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


### Copy
Copy a file or folder to a different location in the user's Dropbox. If the source path is a folder all its contents will be copied.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Filename | STRING | TEXT  |
| Source path | STRING | TEXT  |
| Destination path | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Create new folder
Create a folder at a given path.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Folder path/name | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Create a new paper file
Create a new .paper file on which you can write at a given path

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Paper path/name | STRING | TEXT  |
| Filename | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |





### Delete
Delete the file or folder at a given path. If the path is a folder, all its contents will be deleted too.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Path | STRING | TEXT  |
| Filename | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Metadata | OBJECT | OBJECT_BUILDER  |





### Get file link
Get a temporary link to stream content of a file. This link will expire in four hours and afterwards you will get 410 Gone. This URL should not be used to display content directly in the browser. The Content-Type of the link is determined automatically by the file's mime type.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Path to the file | STRING | TEXT  |
| Filename | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Metadata | OBJECT | OBJECT_BUILDER  |
| Link | STRING | TEXT  |





### List folder
Lists content of a folder.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Path | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |





### Move
Move a file or folder to a different location in the user's Dropbox. If the source path is a folder all its contents will be moved. Note that we do not currently support case-only renaming.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Filename | STRING | TEXT  |
| Source path | STRING | TEXT  |
| Destination path | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Metadata | OBJECT | OBJECT_BUILDER  |





### Search
Searches for files and folders. Can only be used to retrieve a maximum of 10,000 matches. Recent changes may not immediately be reflected in search results due to a short delay in indexing. Duplicate results may be returned across pages. Some results may not be returned.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Search string | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Matches | ARRAY | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |





### Upload file
Create a new file up to a size of 150MB with the contents provided in the request.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Destination path | STRING | TEXT  |
| Filename | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| DATE | DATE  |
| DATE | DATE  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| Sym link info | OBJECT | OBJECT_BUILDER  |
| Sharing info | OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| Export info | OBJECT | OBJECT_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





