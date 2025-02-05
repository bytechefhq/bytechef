---
title: "AWS S3"
description: "AWS S3 is a simple object storage service provided by Amazon Web Services."
---

AWS S3 is a simple object storage service provided by Amazon Web Services.


Categories: developer-tools, file-storage


Type: awsS3/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| accessKeyId | Access Key ID | STRING | TEXT  |  | true  |
| secretAccessKey | Secret Access Key | STRING | TEXT  |  | true  |
| region | STRING | SELECT  |
| bucketName | Bucket | STRING | TEXT  |  | true  |





<hr />



## Actions


### Get Object
Get the AWS S3 object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| filename | Filename | STRING | TEXT  |  Filename to set for binary data.  |  true  |
| key | Key | STRING | TEXT  |  Key is most likely the name of the file.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Get URL
Get the url of an AWS S3 object.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key or Entity Tag (Etag) | STRING | TEXT  |  Key is most likely the name of the file.  |  true  |


#### Output


___Sample Output:___

```https://s3.amazonaws.com/bucket-name/key```



Type: STRING







### List Objects
Get the list AWS S3 objects. Every object needs to have read permission in order to be seen.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| prefix | Prefix | STRING | TEXT  |  The prefix of an AWS S3 objects.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(key), STRING\(suffix), STRING\(uri)} | OBJECT_BUILDER  |






### Get Pre-signed Object
You can share an object with a pre-signed URL for up to 12 hours or until your session expires.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  Key is most likely the name of the file.  |  true  |
| signatureDuration | Signature Duration | STRING | TEXT  |  Time interval until the pre-signed URL expires  |  true  |


#### Output



Type: STRING







### Put Object
Store an object to AWS S3.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| fileEntry | File | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file that needs to be written to AWS S3.  |  true  |
| key | Key | STRING | TEXT  |  Key is most likely the name of the file.  |  true  |
| acl | ACL | STRING | SELECT  |  The canned ACL to apply to the object.  |  null  |


#### Output



Type: STRING









