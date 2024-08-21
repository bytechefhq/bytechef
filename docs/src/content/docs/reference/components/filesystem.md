---
title: "Filesystem"
description: "Allows multiple operations over files on the filesystem."
---
## Reference
<hr />

Allows multiple operations over files on the filesystem.


Categories: [HELPERS]


Version: 1

<hr />






## Actions


### Read File
Reads all data from a specified file path and outputs it in file entry format.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File path | STRING | TEXT  |  The path of the file to read.  |


### Output



Type: FILE_ENTRY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Write to file
null

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  File entry object to be written.  |
| File path | STRING | TEXT  |  The path to which the file should be written.  |


### Output


___Sample Output:___

```{bytes=1024}```



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |






### Create Temp Directory
Creates a file in the temporary directory on the filesystem. Returns the created directory's full path.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```/sample_tmp_dir```



Type: STRING







### Get parent folder
Gets the path of the parent folder of the file. If the file doesn't exist, it throws an error.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File path | STRING | TEXT  |  The path to full filename.  |


### Output


___Sample Output:___

```/sample_data```



Type: STRING







### List
Lists the content of a directory for the given path.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Path | STRING | TEXT  |  The path of a directory.  |
| Recursive | BOOLEAN | SELECT  |  Should the subdirectories be included?  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(fileName), STRING\(relativePath), INTEGER\(size)} | OBJECT_BUILDER  |






### Create
Creates a directory.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Path | STRING | TEXT  |  The path of a directory.  |


### Output


___Sample Output:___

```/sample_data```



Type: STRING







### Remove
Permanently removes the content of a directory.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Path | STRING | TEXT  |  The path of a directory.  |


### Output


___Sample Output:___

```true```



Type: BOOLEAN







