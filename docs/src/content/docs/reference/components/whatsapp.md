---
title: "WhatsApp"
description: "WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and video communication, along with document and media sharing, available on multiple platforms."
---

WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and video communication, along with document and media sharing, available on multiple platforms.


Categories: communication


Type: whatsApp/v1

<hr />



## Connections

Version: 1


### WhatsApp Custom Authorization

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| systemUserAccessToken | System user access token | STRING | TEXT |  | true |
| phoneNumberId | Phone number ID | STRING | TEXT |  | true |





<hr />



## Actions


### Send Message
Name: sendMessage

Send a message via WhatsApp

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| body | Message | STRING | TEXT | Message to send via WhatsApp | true |
| to | Send Message To | STRING | TEXT | Phone number to send the message. It must start with "+" sign | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| messaging_product | STRING | TEXT |
| contacts | OBJECT <details> <summary> Properties </summary> {STRING\(input), STRING\(wa_id)} </details> | OBJECT_BUILDER |
| messages | OBJECT <details> <summary> Properties </summary> {STRING\(id)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Send Message",
  "name" : "sendMessage",
  "parameters" : {
    "body" : "",
    "to" : ""
  },
  "type" : "whatsApp/v1/sendMessage"
}
```




## Triggers


### Message Received
Name: messageReceived

Triggers when you get a new message from certain number.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| senderNumber | Sender Number | STRING | TEXT | Type in the number from whom you want to trigger | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| object | STRING | TEXT |
| entry | OBJECT <details> <summary> Properties </summary> {STRING\(id), {{STRING\(messaging_product), {STRING\(display_phone_number), STRING\(phone_number_id)}\(metadata)}\(value), {{STRING\(name)}\(profile), STRING\(wa_id)}\(contacts), {STRING\(from), STRING\(id), STRING\(timestamp), {STRING\(body)}\(text)}\(messages)}\(changes)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Message Received",
  "name" : "messageReceived",
  "parameters" : {
    "senderNumber" : ""
  },
  "type" : "whatsApp/v1/messageReceived"
}
```


<hr />

