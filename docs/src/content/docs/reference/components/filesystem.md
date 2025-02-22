---
title: "Filesystem"
description: "Allows multiple operations over files on the filesystem."
---

Allows multiple operations over files on the filesystem.


Categories: helpers


Type: filesystem/v1

<hr />




## Actions


### Read File
Name: readFile

Reads all data from a specified file path and outputs it in file entry format.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| filename | File path | STRING | The path of the file to read. | true |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| extension | STRING |
| mimeType | STRING |
| name | STRING |
| url | STRING |




#### JSON Example
```json
{
  "label" : "Read File",
  "name" : "readFile",
  "parameters" : {
    "filename" : ""
  },
  "type" : "filesystem/v1/readFile"
}
```


### Write to File
Name: writeFile

null

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | File entry object to be written. | true |
| filename | File path | STRING | The path to which the file should be written. | true |


#### Output


___Sample Output:___

```{bytes=1024}```



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| bytes | INTEGER |




#### JSON Example
```json
{
  "label" : "Write to File",
  "name" : "writeFile",
  "parameters" : {
    "fileEntry" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    },
    "filename" : ""
  },
  "type" : "filesystem/v1/writeFile"
}
```


### Create Temp Directory
Name: createTempDir

Creates a file in the temporary directory on the filesystem. Returns the created directory's full path.


#### Output


___Sample Output:___

```/sample_tmp_dir```



Type: STRING





#### JSON Example
```json
{
  "label" : "Create Temp Directory",
  "name" : "createTempDir",
  "type" : "filesystem/v1/createTempDir"
}
```


### Get Parent Folder
Name: getFilePath

Gets the path of the parent folder of the file. If the file doesn't exist, it throws an error.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| filename | File path | STRING | The path to full filename. | true |


#### Output


___Sample Output:___

```/sample_data```



Type: STRING





#### JSON Example
```json
{
  "label" : "Get Parent Folder",
  "name" : "getFilePath",
  "parameters" : {
    "filename" : ""
  },
  "type" : "filesystem/v1/getFilePath"
}
```


### List
Name: ls

Lists the content of a directory for the given path.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| path | Path | STRING | The path of a directory. | true |
| recursive | Recursive | BOOLEAN <details> <summary> Options </summary> true, false </details> | Should the subdirectories be included? | null |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(fileName), STRING\(relativePath), INTEGER\(size)} </details> |




#### JSON Example
```json
{
  "label" : "List",
  "name" : "ls",
  "parameters" : {
    "path" : "",
    "recursive" : false
  },
  "type" : "filesystem/v1/ls"
}
```


### Create
Name: mkdir

Creates a directory.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| path | Path | STRING | The path of a directory. | true |


#### Output


___Sample Output:___

```/sample_data```



Type: STRING





#### JSON Example
```json
{
  "label" : "Create",
  "name" : "mkdir",
  "parameters" : {
    "path" : ""
  },
  "type" : "filesystem/v1/mkdir"
}
```


### Remove
Name: rm

Permanently removes the content of a directory.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| path | Path | STRING | The path of a directory. | true |


#### Output


___Sample Output:___

```true```



Type: BOOLEAN





#### JSON Example
```json
{
  "label" : "Remove",
  "name" : "rm",
  "parameters" : {
    "path" : ""
  },
  "type" : "filesystem/v1/rm"
}
```




