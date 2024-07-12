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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |





<hr />





## Actions


### Send an email
Sends an email.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From: | STRING | TEXT  |
| To: | ARRAY | ARRAY_BUILDER  |
| CC: | ARRAY | ARRAY_BUILDER  |
| Subject | STRING | TEXT  |
| Message Body | STRING | TEXT  |
| Message type | STRING | SELECT  |
| Attachments | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|






