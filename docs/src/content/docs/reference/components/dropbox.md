---
title: "Dropbox"
description: "Dropbox is a file hosting service that offers cloud storage, file synchronization, personal cloud, and client software."
---

Dropbox is a file hosting service that offers cloud storage, file synchronization, personal cloud, and client software.


Categories: file-storage


Type: dropbox/v1

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


### Copy
Name: copy

Copy a file or folder to a different location in the user's Dropbox. If the source path is a folder all its contents will be copied.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| filename | Filename | STRING | TEXT | Name of the file with the extension. Don't fill in if you want a folder. | false |
| from_path | Source Path | STRING | TEXT | The path which the file or folder should be copyed from.  Root is /. | true |
| to_path | Destination Path | STRING | TEXT | The path which the file or folder should be copyed to.  Root is /. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| metadata | OBJECT <details> <summary> Properties </summary> {STRING\(.tag), STRING\(name), STRING\(path_lower), STRING\(path_display), STRING\(id)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Copy",
  "name" : "copy",
  "parameters" : {
    "filename" : "",
    "from_path" : "",
    "to_path" : ""
  },
  "type" : "dropbox/v1/copy"
}
```


### Create New Folder
Name: createFolder

Create a folder at a given path.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| path | Folder Path/Name | STRING | TEXT | The path of the new folder. Root is /. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| metadata | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(path_lower), STRING\(path_display), STRING\(id)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create New Folder",
  "name" : "createFolder",
  "parameters" : {
    "path" : ""
  },
  "type" : "dropbox/v1/createFolder"
}
```


### Create New Paper File
Name: createTextFile

Create a new .paper file on which you can write at a given path

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| path | Path | STRING | TEXT | The path of the new paper file. Root is /. | true |
| filename | Filename | STRING | TEXT | Name of the paper file | true |
| text | Text | STRING | TEXT_AREA | The text to write into the file. | true |
| autorename | Auto Rename | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If there's a conflict, as determined by mode, have the Dropbox server try to autorename the file to avoid conflict. | false |
| mute | Mute | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Normally, users are made aware of any file modifications in their Dropbox account via notifications in the client software. If true, this tells the clients that this modification shouldn't result in a user notification. | false |
| strict_conflict | Strict Conflict | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Be more strict about how each WriteMode detects conflict. For example, always return a conflict error when mode = WriteMode.update and the given "rev" doesn't match the existing file's "rev", even if the existing file has been deleted. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| url | STRING | TEXT |
| resultPath | STRING | TEXT |
| fileId | STRING | TEXT |
| paperRevision | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Create New Paper File",
  "name" : "createTextFile",
  "parameters" : {
    "path" : "",
    "filename" : "",
    "text" : "",
    "autorename" : false,
    "mute" : false,
    "strict_conflict" : false
  },
  "type" : "dropbox/v1/createTextFile"
}
```


### Delete
Name: delete

Delete the file or folder at a given path. If the path is a folder, all its contents will be deleted too.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| path | Path | STRING | TEXT | Path of the file or folder. Root is /. | true |
| filename | Filename | STRING | TEXT | Name of the file. Leave empty if you want to delete a folder. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| metadata | OBJECT <details> <summary> Properties </summary> {STRING\(.tag), STRING\(name), STRING\(path_lower), STRING\(path_display), STRING\(id)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Delete",
  "name" : "delete",
  "parameters" : {
    "path" : "",
    "filename" : ""
  },
  "type" : "dropbox/v1/delete"
}
```


### Get File Link
Name: getFileLink

Get a temporary link to stream content of a file. This link will expire in four hours and afterwards you will get 410 Gone. This URL should not be used to display content directly in the browser. The Content-Type of the link is determined automatically by the file's mime type.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| path | Namepath to the File | STRING | TEXT | The path to the file you want a temporary link to.  Root is /. | true |
| filename | Filename | STRING | TEXT | Name of the file with the extension. Needs to have a streamable extension (.mp4, .mov, .webm, ect) | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| metadata | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(path_lower), STRING\(path_display), STRING\(id)} </details> | OBJECT_BUILDER |
| link | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Get File Link",
  "name" : "getFileLink",
  "parameters" : {
    "path" : "",
    "filename" : ""
  },
  "type" : "dropbox/v1/getFileLink"
}
```


### List Folder
Name: listFolder

List the contents of a folder.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| path | Path | STRING | TEXT | Path of the filename. Inputting nothing searches root. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| entries | ARRAY <details> <summary> Items </summary> [{{STRING\(.tag), STRING\(name), STRING\(path_lower), STRING\(path_Display), STRING\(id)}\(f)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "List Folder",
  "name" : "listFolder",
  "parameters" : {
    "path" : ""
  },
  "type" : "dropbox/v1/listFolder"
}
```


### Move
Name: move

Move a file or folder to a different location in the user's Dropbox. If the source path is a folder all its contents will be moved. Note that we do not currently support case-only renaming.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| filename | Filename | STRING | TEXT | Name of the file with the extension. Don't fill in if you want a folder. | false |
| from_path | Source Path | STRING | TEXT | Path in the user's Dropbox to be moved.  Root is /. | true |
| to_path | Destination Path | STRING | TEXT | Path in the user's Dropbox that is the destination. Root is /. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| metadata | OBJECT <details> <summary> Properties </summary> {STRING\(.tag), STRING\(name), STRING\(path_lower), STRING\(path_display), STRING\(id)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Move",
  "name" : "move",
  "parameters" : {
    "filename" : "",
    "from_path" : "",
    "to_path" : ""
  },
  "type" : "dropbox/v1/move"
}
```


### Search
Name: search

Searches for files and folders. Can only be used to retrieve a maximum of 10,000 matches. Recent changes may not immediately be reflected in search results due to a short delay in indexing. Duplicate results may be returned across pages. Some results may not be returned.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| query | Search String | STRING | TEXT | The string to search for. May match across multiple fields based on the request arguments. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| matches | ARRAY <details> <summary> Items </summary> [{{STRING\(.tag)}\(match_type), {STRING\(.tag), STRING\(id), STRING\(name), STRING\(path_display), STRING\(path_lower)}\(metadata)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Search",
  "name" : "search",
  "parameters" : {
    "query" : ""
  },
  "type" : "dropbox/v1/search"
}
```


### Upload File
Name: uploadFile

Create a new file up to a size of 150MB with the contents provided in the request.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY | The object property which contains a reference to the file to be written. | true |
| path | Destination Path | STRING | TEXT | The path to which the file should be written. | true |
| filename | Filename | STRING | TEXT | Name of the file. Needs to have the appropriate extension. | true |
| autorename | Auto Rename | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If there's a conflict, as determined by mode, have the Dropbox server try to autorename the file to avoid conflict. | false |
| mute | Mute | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Normally, users are made aware of any file modifications in their Dropbox account via notifications in the client software. If true, this tells the clients that this modification shouldn't result in a user notification. | false |
| strict_conflict | Strict Conflict | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Be more strict about how each WriteMode detects conflict. For example, always return a conflict error when mode = WriteMode.update and the given "rev" doesn't match the existing file's "rev", even if the existing file has been deleted. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| clientModified | DATE | DATE |
| serverModified | DATE | DATE |
| rev | STRING | TEXT |
| size | INTEGER | INTEGER |
| symlinkInfo | OBJECT <details> <summary> Properties </summary> {STRING\(target)} </details> | OBJECT_BUILDER |
| sharingInfo | OBJECT <details> <summary> Properties </summary> {STRING\(parentSharedFolderId), STRING\(modifiedBy)} </details> | OBJECT_BUILDER |
| isDownloadable | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| exportInfo | OBJECT <details> <summary> Properties </summary> {STRING\(exportAs), [STRING]\(exportOptions)} </details> | OBJECT_BUILDER |
| propertyGroups | ARRAY <details> <summary> Items </summary> [{STRING\(templateId), [{STRING\(name), STRING\(value)}]\(fields)}] </details> | ARRAY_BUILDER |
| hasExplicitSharedMembers | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| contentHash | STRING | TEXT |
| fileLockInfo | OBJECT <details> <summary> Properties </summary> {BOOLEAN\(isLockholder), STRING\(lockholderName), STRING\(lockholderAccountId), DATE\(created)} </details> | OBJECT_BUILDER |




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
    "path" : "",
    "filename" : "",
    "autorename" : false,
    "mute" : false,
    "strict_conflict" : false
  },
  "type" : "dropbox/v1/uploadFile"
}
```




<hr />

# Additional instructions
<hr />

![anl-c-dropbox-md](https://static.scarf.sh/a.png?x-pxid=8999e724-f122-49be-a4bb-139c6576aec3)
## CONNECTION

[Setting up OAuth2](https://developers.dropbox.com/oauth-guide)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.81250000% + 32px)"><iframe src="https://www.guidejar.com/embed/756fb792-9de7-4ac9-b58a-c8c8a95fab66?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
