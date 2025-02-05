---
title: "Resend"
description: "Resend is the email API for developers."
---

Resend is the email API for developers.


Categories: marketing-automation


Type: resend/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Send Email
Send an email

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from | From | STRING | EMAIL  |  Sender email address.  |  true  |
| to | To | [STRING\($email)] | ARRAY_BUILDER  |  Recipients email addresses.  |  true  |
| subject | Subject | STRING | TEXT  |  Email subject.  |  true  |
| bcc | Bcc | [STRING\($email)] | ARRAY_BUILDER  |  Bcc recipients email addresses.  |  false  |
| cc | Cc | [STRING\($email)] | ARRAY_BUILDER  |  Cc recipients email addresses.  |  false  |
| reply_to | Reply To | [STRING\($email)] | ARRAY_BUILDER  |  Reply-to email addresses.  |  false  |
| contentType | Content Type | STRING | SELECT  |  | true  |
| html | HTML | STRING | RICH_TEXT  |  The HTML version of the message.  |  false  |
| text | Text | STRING | TEXT_AREA  |  The plain text version of the message.  |  false  |
| headers | Headers | {} | OBJECT_BUILDER  |  Custom headers to add to the email.  |  false  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |  false  |
| tags | [{STRING\(name), STRING\(value)}] | ARRAY_BUILDER  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |








<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://resend.com/api-keys)
