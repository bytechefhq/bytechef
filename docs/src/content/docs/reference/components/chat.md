---
title: "Chat"
description: "Actions and triggers for using with the chat widget."
---

Actions and triggers for using with the chat widget.


Categories: helpers


Type: chat/v1

<hr />




## Actions


### Response to Chat Request
Converts the response to chat request.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| message | Message | STRING | TEXT  |  The message of the response.  |  null  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  The attachments of the response.  |  null  |






## Triggers


### New Chat Request
.

Type: STATIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| mode | INTEGER | SELECT  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| conversationId | STRING | TEXT  |
| message | STRING | TEXT  |
| attachments | [FILE_ENTRY] | ARRAY_BUILDER  |







<hr />

