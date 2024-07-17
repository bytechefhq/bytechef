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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| System user access token | STRING | TEXT  |  |
| Phone number ID | STRING | TEXT  |  |





<hr />



## Triggers


### Message received
Triggers when you get a new message from certain number.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Sender number | STRING | TEXT  |  Type in the number from whom you want to trigger  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| {STRING(id), {{STRING(messaging_product), {STRING(display_phone_number), STRING(phone_number_id)}(metadata)}(value), {{STRING(name)}(profile), STRING(wa_id)}(contacts), {STRING(from), STRING(id), STRING(timestamp), {STRING(body)}(text)}(messages)}(changes)} | OBJECT_BUILDER  |






<hr />



## Actions


### Send Message
Send a message via WhatsApp

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message | STRING | TEXT  |  Message to send via WhatsApp  |
| Send message to | STRING | TEXT  |  Phone number to send the message. It must start with "+" sign  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| {STRING(input), STRING(wa_id)} | OBJECT_BUILDER  |
| {STRING(id)} | OBJECT_BUILDER  |





