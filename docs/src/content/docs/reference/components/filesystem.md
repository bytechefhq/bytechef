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
Reads all data from a specified file path and outputs it in file entry format.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| filename | File path | STRING | TEXT  |  The path of the file to read.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Write to File
null

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY  |  File entry object to be written.  |  true  |
| filename | File path | STRING | TEXT  |  The path to which the file should be written.  |  true  |


#### Output


___Sample Output:___

```{bytes=1024}```



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| bytes | INTEGER | INTEGER  |






### Create Temp Directory
Creates a file in the temporary directory on the filesystem. Returns the created directory's full path.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output


___Sample Output:___

```/sample_tmp_dir```



Type: STRING







### Get Parent Folder
Gets the path of the parent folder of the file. If the file doesn't exist, it throws an error.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| filename | File path | STRING | TEXT  |  The path to full filename.  |  true  |


#### Output


___Sample Output:___

```/sample_data```



Type: STRING







### List
Lists the content of a directory for the given path.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| path | Path | STRING | TEXT  |  The path of a directory.  |  true  |
| recursive | Recursive | BOOLEAN | SELECT  |  Should the subdirectories be included?  |  null  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(fileName), STRING\(relativePath), INTEGER\(size)} | OBJECT_BUILDER  |






### Create
Creates a directory.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| path | Path | STRING | TEXT  |  The path of a directory.  |  true  |


#### Output


___Sample Output:___

```/sample_data```



Type: STRING







### Remove
Permanently removes the content of a directory.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| path | Path | STRING | TEXT  |  The path of a directory.  |  true  |


#### Output


___Sample Output:___

```true```



Type: BOOLEAN









