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


### Read from file
null

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Filename | STRING | TEXT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Filename | STRING | TEXT  |


### Output


___Sample Output:___

```{bytes=1024}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |





### Create Temp Directory
Creates a temporary directory on the filesystem.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
null


### Output


___Sample Output:___

```/sample_tmp_dir```



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### File Path
Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory separator.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Filename | STRING | TEXT  |


### Output


___Sample Output:___

```/sample_data```



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### List
Lists a content of directory for the given path.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Path | STRING | TEXT  |
| Recursive | BOOLEAN | SELECT  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Create
Creates a directory.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Path | STRING | TEXT  |


### Output


___Sample Output:___

```/sample_data```



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Remove
Removes the content of a directory.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Path | STRING | TEXT  |


### Output


___Sample Output:___

```true```



Type: BOOLEAN

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





