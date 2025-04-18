---
title: "WhatsApp"
description: "WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and video communication, along with document and media sharing, available on multiple platforms."
---

WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and video communication, along with document and media sharing, available on multiple platforms.


Categories: Communication


Type: whatsApp/v1

<hr />



## Connections

Version: 1


### WhatsApp Custom Authorization

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| systemUserAccessToken | System user access token | STRING |  | true |
| phoneNumberId | Phone number ID | STRING |  | true |





<hr />



## Actions


### Send Message
Name: sendMessage

Send a message via WhatsApp

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| body | Message | STRING | Message to send via WhatsApp | true |
| to | Send Message To | STRING | Phone number to send the message. It must start with "+" sign | true |

#### Example JSON Structure
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

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| messaging_product | STRING |  |
| contacts | OBJECT <details> <summary> Properties </summary> {STRING\(input), STRING\(wa_id)} </details> |  |
| messages | OBJECT <details> <summary> Properties </summary> {STRING\(id)} </details> |  |




#### Output Example
```json
{
  "messaging_product" : "",
  "contacts" : {
    "input" : "",
    "wa_id" : ""
  },
  "messages" : {
    "id" : ""
  }
}
```




## Triggers


### Message Received
Name: messageReceived

Triggers when you get a new message from certain number.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| senderNumber | Sender Number | STRING | Type in the number from whom you want to trigger | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| object | STRING |  |
| entry | OBJECT <details> <summary> Properties </summary> {STRING\(id), {{STRING\(messaging_product), {STRING\(display_phone_number), STRING\(phone_number_id)}\(metadata)}\(value), {{STRING\(name)}\(profile), STRING\(wa_id)}\(contacts), {STRING\(from), STRING\(id), STRING\(timestamp), {STRING\(body)}\(text)}\(messages)}\(changes)} </details> |  |




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

