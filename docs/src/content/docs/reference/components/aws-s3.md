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


### custom

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| accessKeyId | Access Key ID | STRING |  | true |
| secretAccessKey | Secret Access Key | STRING |  | true |
| region | | STRING <details> <summary> Options </summary> us-east-1, us-east-2, us-west-1, us-west-2, ca-central-1, ap-east-1, ap-south-1, ap-south-2, ap-northeast-3, ap-northeast-2, ap-southeast-1, ap-southeast-2, ap-southeast-3, ap-southeast-4, ap-northeast-1, me-south-1, me-central-1, eu-central-1, eu-central-2, eu-west-1, eu-west-2, eu-south-1, eu-south-2, eu-west-3, eu-north-1, af-south-1, sa-east-1, cn-north-1, cn-northwest-1 </details> |  | true |
| bucketName | Bucket | STRING |  | true |





<hr />



## Actions


### Get Object
Name: getObject

Get the AWS S3 object.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| filename | Filename | STRING | Filename to set for binary data. | true |
| key | Key | STRING | Key is most likely the name of the file. | true |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| extension | STRING |  |
| mimeType | STRING |  |
| name | STRING |  |
| url | STRING |  |




#### JSON Example
```json
{
  "label" : "Get Object",
  "name" : "getObject",
  "parameters" : {
    "filename" : "",
    "key" : ""
  },
  "type" : "awsS3/v1/getObject"
}
```


### Get URL
Name: getUrl

Get the url of an AWS S3 object.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key or Entity Tag (Etag) | STRING | Key is most likely the name of the file. | true |


#### Output


___Sample Output:___

```https://s3.amazonaws.com/bucket-name/key```



Type: STRING





#### JSON Example
```json
{
  "label" : "Get URL",
  "name" : "getUrl",
  "parameters" : {
    "key" : ""
  },
  "type" : "awsS3/v1/getUrl"
}
```


### List Objects
Name: listObjects

Get the list AWS S3 objects. Every object needs to have read permission in order to be seen.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| prefix | Prefix | STRING | The prefix of an AWS S3 objects. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(key), STRING\(suffix), STRING\(uri)} </details> |  |




#### JSON Example
```json
{
  "label" : "List Objects",
  "name" : "listObjects",
  "parameters" : {
    "prefix" : ""
  },
  "type" : "awsS3/v1/listObjects"
}
```


### Get Pre-signed Object
Name: presignGetObject

You can share an object with a pre-signed URL for up to 12 hours or until your session expires.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING | Key is most likely the name of the file. | true |
| signatureDuration | Signature Duration | STRING | Time interval until the pre-signed URL expires | true |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "Get Pre-signed Object",
  "name" : "presignGetObject",
  "parameters" : {
    "key" : "",
    "signatureDuration" : ""
  },
  "type" : "awsS3/v1/presignGetObject"
}
```


### Put Object
Name: putObject

Store an object to AWS S3.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| fileEntry | File | FILE_ENTRY | The object property which contains a reference to the file that needs to be written to AWS S3. | true |
| key | Key | STRING | Key is most likely the name of the file. | true |
| acl | ACL | STRING <details> <summary> Options </summary> authenticated-read, aws-exec-read, bucket-owner-read, bucket-owner-full-control, private, public-read, public-read-write </details> | The canned ACL to apply to the object. | null |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "Put Object",
  "name" : "putObject",
  "parameters" : {
    "fileEntry" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    },
    "key" : "",
    "acl" : ""
  },
  "type" : "awsS3/v1/putObject"
}
```




