---
title: "Email"
description: "The Email connector sends emails using an SMTP email server."
---
## Reference
<hr />

The Email connector sends emails using an SMTP email server.


Categories: [COMMUNICATION, HELPERS]


Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |





<hr />





## Actions


### Send
Send an email to any address.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From Email | INTEGER | INTEGER  |  From who to send the email.  |
| To Email | [STRING] | ARRAY_BUILDER  |  Who to send the email to.  |
| CC Email | [STRING] | ARRAY_BUILDER  |  Who to CC on the email.  |
| BCC Email | [STRING] | ARRAY_BUILDER  |  Who to BCC on the email.  |
| Reply To | [STRING] | ARRAY_BUILDER  |  When someone replies to this email, where should it go to?  |
| Subject | STRING | TEXT  |  Your email subject.  |
| Content | STRING | TEXT  |  Your email content. Will be sent as a HTML email.  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |




