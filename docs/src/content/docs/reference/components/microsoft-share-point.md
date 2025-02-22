---
title: "Microsoft SharePoint"
description: "Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations."
---

Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, providing document management, intranet, and content management features for organizations.


Categories: file-storage, communication


Type: microsoftSharePoint/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |
| tenantId | Tenant Id | STRING |  | true |





<hr />



## Actions


### Create Folder
Name: createFolder

Creates a new folder at path you specify.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING |  | true |
| parentFolder | Parent Folder ID | STRING <details> <summary> Depends On </summary> siteId </details> | If no folder is selected, file will be uploaded to root folder. | false |
| name | Folder Name | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "Create Folder",
  "name" : "createFolder",
  "parameters" : {
    "siteId" : "",
    "parentFolder" : "",
    "name" : ""
  },
  "type" : "microsoftSharePoint/v1/createFolder"
}
```


### Create List
Name: createList

Creates a new list

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING |  | true |
| displayName | List Name | STRING |  | true |
| description | List Description | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| description | STRING |
| displayName | STRING |




#### JSON Example
```json
{
  "label" : "Create List",
  "name" : "createList",
  "parameters" : {
    "siteId" : "",
    "displayName" : "",
    "description" : ""
  },
  "type" : "microsoftSharePoint/v1/createList"
}
```


### Create List Item
Name: createListItem

Creates a new item in a list.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING |  | true |
| listId | List ID | STRING <details> <summary> Depends On </summary> siteId </details> |  | true |
| columns | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> siteId, listId </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |




#### JSON Example
```json
{
  "label" : "Create List Item",
  "name" : "createListItem",
  "parameters" : {
    "siteId" : "",
    "listId" : "",
    "columns" : { }
  },
  "type" : "microsoftSharePoint/v1/createListItem"
}
```


### Upload File
Name: uploadFile

Upload file to Microsoft SharePoint folder.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING |  | true |
| parentFolder | Parent Folder ID | STRING <details> <summary> Depends On </summary> siteId </details> | If no folder is selected, file will be uploaded to root folder | false |
| file | File | FILE_ENTRY | File to upload. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |




#### JSON Example
```json
{
  "label" : "Upload File",
  "name" : "uploadFile",
  "parameters" : {
    "siteId" : "",
    "parentFolder" : "",
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "microsoftSharePoint/v1/uploadFile"
}
```




