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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Username | STRING | TEXT  |
| Password | STRING | TEXT  |





<hr />





## Actions


### Send
Send an email to any address.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From Email | INTEGER | INTEGER  |
| To Email | ARRAY | ARRAY_BUILDER  |
| CC Email | ARRAY | ARRAY_BUILDER  |
| BCC Email | ARRAY | ARRAY_BUILDER  |
| Reply To | ARRAY | ARRAY_BUILDER  |
| Subject | STRING | TEXT  |
| Content | STRING | TEXT  |
| Attachments | ARRAY | ARRAY_BUILDER  |




