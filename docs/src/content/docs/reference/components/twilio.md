---
title: "Twilio"
description: "Twilio is a cloud communications platform that enables developers to integrate messaging, voice, and video capabilities into their applications."
---
## Reference
<hr />

Twilio is a cloud communications platform that enables developers to integrate messaging, voice, and video capabilities into their applications.

Categories: [COMMUNICATION]

Version: 1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Account SID | STRING | TEXT  |
| Auth Token | STRING | TEXT  |





<hr />





## Actions


### Send SMS
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Account SID | STRING | TEXT  |
| To | STRING | PHONE  |
| Source | INTEGER | SELECT  |
| From | STRING | PHONE  |
| Messaging Service SID | STRING | TEXT  |
| Content | INTEGER | SELECT  |
| Body | STRING | TEXT  |
| Media URL | ARRAY | ARRAY_BUILDER  |
| Status callback | STRING | URL  |
| Application SID | STRING | TEXT  |
| Maximum price | NUMBER | NUMBER  |
| Provide feedback | BOOLEAN | SELECT  |
| Attempt | INTEGER | INTEGER  |
| Validity period | INTEGER | INTEGER  |
| Force delivery | BOOLEAN | SELECT  |
| Content retention | STRING | SELECT  |
| Address retention | STRING | SELECT  |
| Smart encoded | BOOLEAN | SELECT  |
| Persistent action | ARRAY | ARRAY_BUILDER  |
| Shorten URLs | BOOLEAN | SELECT  |
| Schedule type | STRING | SELECT  |
| Send at | OBJECT | OBJECT_BUILDER  |
| Send as MMS | BOOLEAN | SELECT  |
| Content variables | STRING | TEXT  |
| Risk check | STRING | SELECT  |
| Content SID | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





