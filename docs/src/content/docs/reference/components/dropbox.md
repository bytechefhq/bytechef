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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />





## Actions


### Copy
Copy a file or folder to a different location in the user's Dropbox. If the source path is a folder all its contents will be copied.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Filename | STRING | TEXT  |  Name of the file with the extension. Don't fill in if you want a folder.  |
| Source path | STRING | TEXT  |  The path which the file or folder should be copyed from.  Root is /.  |
| Destination path | STRING | TEXT  |  The path which the file or folder should be copyed to.  Root is /.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(name), STRING(pathLower), STRING(pathDisplay), STRING(parentSharedFolderId), STRING(previewUrl)} | OBJECT_BUILDER  |





### Create new folder
Create a folder at a given path.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Folder path/name | STRING | TEXT  |  The path of the new folder. Root is /.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(sharedFolderId), {STRING(parentSharedFolderId), STRING(sharedFolderId), BOOLEAN(traverseOnly), BOOLEAN(noAccess), [{STRING(templateId), [{STRING(name), STRING(value)}](fields)}](propertyGroups)}(sharingInfo)} | OBJECT_BUILDER  |





### Create a new paper file
Create a new .paper file on which you can write at a given path

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Paper path/name | STRING | TEXT  |  The path of the new paper file. Starts with / as root.  |
| Filename | STRING | TEXT  |  Name of the paper file  |


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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Path | STRING | TEXT  |  Path of the file or folder. Root is /.  |
| Filename | STRING | TEXT  |  Name of the file. Leave empty if you want to delete a folder.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Metadata | {STRING(name), STRING(pathLower), STRING(pathDisplay), STRING(parentSharedFolderId), STRING(previewUrl)} | OBJECT_BUILDER  |  |





### Get file link
Get a temporary link to stream content of a file. This link will expire in four hours and afterwards you will get 410 Gone. This URL should not be used to display content directly in the browser. The Content-Type of the link is determined automatically by the file's mime type.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Path to the file | STRING | TEXT  |  The path to the file you want a temporary link to.  Root is /.  |
| Filename | STRING | TEXT  |  Name of the file with the extension. Needs to have a streamable extension (.mp4, .mov, .webm, ect)  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Metadata | {STRING(id), DATE(clientModified), DATE(serverModified), STRING(rev), INTEGER(size), {STRING(target)}(symlinkInfo), {STRING(parentSharedFolderId), STRING(modifiedBy)}(sharingInfo), BOOLEAN(isDownloadable), {STRING(exportAs), [STRING](exportOptions)}(exportInfo), [{STRING(templateId), [{STRING(name), STRING(value)}](fields)}](propertyGroups), BOOLEAN(hasExplicitSharedMembers), STRING(contentHash), {BOOLEAN(isLockholder), STRING(lockholderName), STRING(lockholderAccountId), DATE(created)}(fileLockInfo)} | OBJECT_BUILDER  |  |
| Link | STRING | TEXT  |  |





### List folder
Lists content of a folder.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Path | STRING | TEXT  |  Path of the filename. Inputting nothing searches root.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{{STRING(name), STRING(pathLower), STRING(pathDisplay), STRING(parentSharedFolderId), STRING(previewUrl)}(metadata)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |





### Move
Move a file or folder to a different location in the user's Dropbox. If the source path is a folder all its contents will be moved. Note that we do not currently support case-only renaming.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Filename | STRING | TEXT  |  Name of the file with the extension. Don't fill in if you want a folder.  |
| Source path | STRING | TEXT  |  Path in the user's Dropbox to be moved.  Root is /.  |
| Destination path | STRING | TEXT  |  Path in the user's Dropbox that is the destination. Root is /.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Metadata | {STRING(name), STRING(pathLower), STRING(pathDisplay), STRING(parentSharedFolderId), STRING(previewUrl)} | OBJECT_BUILDER  |  |





### Search
Searches for files and folders. Can only be used to retrieve a maximum of 10,000 matches. Recent changes may not immediately be reflected in search results due to a short delay in indexing. Duplicate results may be returned across pages. Some results may not be returned.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Search string | STRING | TEXT  |  The string to search for. May match across multiple fields based on the request arguments.Must have length of at most 1000 and not be null.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Matches | [{[{STRING(highlightStr), BOOLEAN(isHighlighted)}](highlightSpans)}] | ARRAY_BUILDER  |  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |





### Upload file
Create a new file up to a size of 150MB with the contents provided in the request.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to be written.  |
| Destination path | STRING | TEXT  |  The path to which the file should be written.  |
| Filename | STRING | TEXT  |  Name of the file. Needs to have the appropriate extension.  |


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
| Sym link info | {STRING(target)} | OBJECT_BUILDER  |  |
| Sharing info | {STRING(parentSharedFolderId), STRING(modifiedBy)} | OBJECT_BUILDER  |  |
| BOOLEAN | SELECT  |
| Export info | {STRING(exportAs), [STRING](exportOptions)} | OBJECT_BUILDER  |  |
| [{STRING(templateId), [{STRING(name), STRING(value)}](fields)}] | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| {BOOLEAN(isLockholder), STRING(lockholderName), STRING(lockholderAccountId), DATE(created)} | OBJECT_BUILDER  |





<hr />

# Additional instructions
<hr />

![anl-c-dropbox-md](https://static.scarf.sh/a.png?x-pxid=8999e724-f122-49be-a4bb-139c6576aec3)
## CONNECTION

[Setting up OAuth2](https://developers.dropbox.com/oauth-guide)

[Guidejar](https://guidejar.com/guides/756fb792-9de7-4ac9-b58a-c8c8a95fab66) tutorial.
