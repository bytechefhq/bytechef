---
id: aws-s3
title: AWS S3
---

TODO

[s3/getObject](src/main/java/com/ByteChef/atlas/task/handler/s3/S3GetObject.java)

```
  type: s3/getObject
  uri: s3://my-bucket/path/to/file.mp4
  filepath: /path/to/my/file.mp4
```

[s3/listObjects](src/main/java/com/ByteChef/atlas/task/handler/s3/S3ListObjects.java)

```
  type: s3/listObjects
  bucket: my-bucket
  prefix: some/path/
```

[s3/getUrl](src/main/java/com/ByteChef/atlas/task/handler/s3/S3GetUrl.java)

```
  type: s3/getUrl
  uri: s3://my-bucket/path/to/file.mp4
```

[s3/presignGetObject](src/main/java/com/ByteChef/atlas/task/handler/s3/S3PresignedGetObject.java)

```
  name: url
  type: s3/presignGetObject
  uri: s3://my-bucket/path/to/file.mp4
  signatureDuration: 60s
```

[s3/putObject](src/main/java/com/ByteChef/atlas/task/handler/s3/S3PutObject.java)

```
  type: s3/putObject
  uri: s3://my-bucket/path/to/file.mp4
  filepath: /path/to/my/file.mp4
```
