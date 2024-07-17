---
title: "Sendgrid"
description: "Trusted for reliable email delivery at scale."
---
## Reference
<hr />

Trusted for reliable email delivery at scale.


Categories: [COMMUNICATION, MARKETING_AUTOMATION]


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


### Send an email
Sends an email.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From: | STRING | TEXT  |  Email address from which you want to send.  |
| To: | [STRING] | ARRAY_BUILDER  |  Email addresses which you want to send to.  |
| CC: | [STRING] | ARRAY_BUILDER  |  Email address which receives a copy.  |
| Subject | STRING | TEXT  |  Subject of your email  |
| Message Body | STRING | TEXT  |  This is the message you want to send  |
| Message type | STRING | SELECT  |  Message type for your content  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments you want to include with the email.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|






