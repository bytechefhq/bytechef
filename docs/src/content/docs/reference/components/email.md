---
title: "Email"
description: "The Email connector sends emails using an SMTP email server."
---

The Email connector sends emails using an SMTP email server.


Categories: communication, helpers


Type: email/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  | true  |
| password | Password | STRING | TEXT  |  | true  |





<hr />



## Actions


### Send
Send an email to any address.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from | From Email | STRING | TEXT  |  From who to send the email.  |  true  |
| to | To Email | [STRING] | ARRAY_BUILDER  |  Who to send the email to.  |  true  |
| cc | CC Email | [STRING] | ARRAY_BUILDER  |  Who to CC on the email.  |  null  |
| bcc | BCC Email | [STRING] | ARRAY_BUILDER  |  Who to BCC on the email.  |  null  |
| replyTo | Reply To | [STRING] | ARRAY_BUILDER  |  When someone replies to this email, where should it go to?  |  null  |
| subject | Subject | STRING | TEXT  |  Your email subject.  |  true  |
| content | Content | STRING | RICH_TEXT  |  Your email content. Will be sent as a HTML email.  |  null  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |  null  |






