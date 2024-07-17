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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |





<hr />





## Actions


### Send Email
Description

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | EMAIL  |  Sender email address.  |
| To | [STRING($email)] | ARRAY_BUILDER  |  Recipients email addresses.  |
| Subject | STRING | TEXT  |  Email subject.  |
| Bcc | [STRING($email)] | ARRAY_BUILDER  |  Bcc recipients email addresses.  |
| Cc | [STRING($email)] | ARRAY_BUILDER  |  Cc recipients email addresses.  |
| Reply to | [STRING($email)] | ARRAY_BUILDER  |  Reply-to email addresses.  |
| Content type | INTEGER | SELECT  |  |
| HTML | STRING | TEXT  |  The HTML version of the message.  |
| Text | STRING | TEXT  |  The plain text version of the message.  |
| Headers | {} | OBJECT_BUILDER  |  Custom headers to add to the email.  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |
| [{STRING(name), STRING(value)}] | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |





