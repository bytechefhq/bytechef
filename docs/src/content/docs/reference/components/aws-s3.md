---
title: "AWS S3"
description: "AWS S3 is a simple object storage service provided by Amazon Web Services."
---
## Reference
<hr />

AWS S3 is a simple object storage service provided by Amazon Web Services.

Categories: [DEVELOPER_TOOLS, FILE_STORAGE]

Version: 1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Access Key ID | STRING | TEXT  |
| Secret Access Key | STRING | TEXT  |
| STRING | SELECT  |
| Bucket | STRING | TEXT  |





<hr />





## Actions


### Get Object
Get the AWS S3 object.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Filename | STRING | TEXT  |
| Key | STRING | TEXT  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Get URL
Get the url of an AWS S3 object.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key or Entity Tag (Etag) | STRING | TEXT  |


### Output



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### List Objects
Get the list AWS S3 objects. Every object needs to have read permission in order to be seen.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Prefix | STRING | TEXT  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Get Pre-signed Object
You can share an object with a pre-signed URL for up to 12 hours or until your session expires.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Signature Duration | STRING | TEXT  |


### Output



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Put Object
Store an object to AWS S3.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Key | STRING | TEXT  |
| ACL | STRING | SELECT  |


### Output



Type: STRING

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





