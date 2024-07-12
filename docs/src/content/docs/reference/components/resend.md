---
title: "Resend"
description: "Resend is the email API for developers."
---
## Reference
<hr />

Resend is the email API for developers.


Categories: [MARKETING_AUTOMATION]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |





<hr />





## Actions


### Send Email
Description

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From | STRING | EMAIL  |
| To | ARRAY | ARRAY_BUILDER  |
| Subject | STRING | TEXT  |
| Bcc | ARRAY | ARRAY_BUILDER  |
| Cc | ARRAY | ARRAY_BUILDER  |
| Reply to | ARRAY | ARRAY_BUILDER  |
| Content type | INTEGER | SELECT  |
| HTML | STRING | TEXT  |
| Text | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Attachments | ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |





