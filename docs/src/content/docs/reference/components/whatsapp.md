---
title: "WhatsApp"
description: "WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and video communication, along with document and media sharing, available on multiple platforms."
---
## Reference
<hr />

WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and video communication, along with document and media sharing, available on multiple platforms.


Categories: [COMMUNICATION]


Version: 1

<hr />



## Connections

Version: 1


### WhatsApp Custom Authorization

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| System user access token | STRING | TEXT  |
| Phone number ID | STRING | TEXT  |





<hr />



## Triggers


### Message received
Triggers when you get a new message from certain number.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Sender number | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |






<hr />



## Actions


### Send Message
Send a message via WhatsApp

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Message | STRING | TEXT  |
| Send message to | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





