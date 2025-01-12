---
title: "Resend"
description: "Resend is the email API for developers."
---
## Reference
<hr />

Resend is the email API for developers.


Categories: [marketing-automation]


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
Send an email

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | EMAIL  |  Sender email address.  |
| To | [STRING\($email)] | ARRAY_BUILDER  |  Recipients email addresses.  |
| Subject | STRING | TEXT  |  Email subject.  |
| Bcc | [STRING\($email)] | ARRAY_BUILDER  |  Bcc recipients email addresses.  |
| Cc | [STRING\($email)] | ARRAY_BUILDER  |  Cc recipients email addresses.  |
| Reply To | [STRING\($email)] | ARRAY_BUILDER  |  Reply-to email addresses.  |
| Content Type | INTEGER | SELECT  |  |
| HTML | STRING | RICH_TEXT  |  The HTML version of the message.  |
| Text | STRING | TEXT_AREA  |  The plain text version of the message.  |
| Headers | {} | OBJECT_BUILDER  |  Custom headers to add to the email.  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |
| [{STRING\(name), STRING\(value)}] | ARRAY_BUILDER  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |






<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://resend.com/api-keys)
