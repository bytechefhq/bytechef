---
title: "Sendgrid"
description: "Trusted for reliable email delivery at scale."
---

Trusted for reliable email delivery at scale.


Categories: communication, marketing-automation


Type: sendgrid/v1

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
Sends an email.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from | From | STRING | TEXT  |  Email address from which you want to send.  |  true  |
| to | To | [STRING] | ARRAY_BUILDER  |  Email addresses which you want to send to.  |  true  |
| cc | CC | [STRING] | ARRAY_BUILDER  |  Email address which receives a copy.  |  false  |
| subject | Subject | STRING | TEXT  |  Subject of your email  |  true  |
| text | Message Body | STRING | RICH_TEXT  |  This is the message you want to send  |  true  |
| type | Message Type | STRING | SELECT  |  Message type for your content  |  true  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments you want to include with the email.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| type | STRING | TEXT  |
| from | STRING | TEXT  |
| to | [STRING] | ARRAY_BUILDER  |
| subject | STRING | TEXT  |
| text | STRING | TEXT  |
| attachments | [FILE_ENTRY] | ARRAY_BUILDER  |








