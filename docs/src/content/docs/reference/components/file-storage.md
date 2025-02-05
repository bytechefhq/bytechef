---
title: "File Storage"
description: "Reads and writes data from a file stored inside the file storage."
---

Reads and writes data from a file stored inside the file storage.


Categories: file-storage, helpers


Type: fileStorage/v1

<hr />




## Actions


### Read from File as String
Reads data from the file as string.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to read from.  |  true  |


#### Output


___Sample Output:___

```Sample content```



Type: STRING







### Write to File
Writes the data to the file.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| content | Content | STRING | TEXT  |  String to write to the file.  |  true  |
| filename | Filename | STRING | TEXT  |  Filename to set for data. By default, "file.txt" will be used.  |  null  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Download File
Download a file from the URL.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| url | URL | STRING | TEXT  |  The URL to download a file from.  |  true  |
| filename | Filename | STRING | TEXT  |  Filename to set for data. By default, "file.txt" will be used.  |  null  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |








