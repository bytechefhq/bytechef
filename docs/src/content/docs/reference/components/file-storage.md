---
title: "File Storage"
description: "Reads and writes data from a file stored inside the file storage."
---
## Reference
<hr />

Reads and writes data from a file stored inside the file storage.

Categories: [FILE_STORAGE, HELPERS]

Version: 1

<hr />






## Actions


### Read from file as string
Reads data from the file as string.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |


### Output


___Sample Output:___

```Sample content```



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Write to file
Writes the data to the file.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Content | STRING | TEXT  |
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





### Download file
Download a file from the URL.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URL | STRING | TEXT  |
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





