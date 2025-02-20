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
Name: responseToRequest

Converts the response to chat request.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| message | Message | STRING | TEXT | The message of the response. | null |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | ARRAY_BUILDER | The attachments of the response. | null |


#### JSON Example
```json
{
  "label" : "Response to Chat Request",
  "name" : "responseToRequest",
  "parameters" : {
    "message" : "",
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ]
  },
  "type" : "chat/v1/responseToRequest"
}
```




## Triggers


### New Chat Request
Name: newChatRequest

.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| mode | | INTEGER <details> <summary> Options </summary> 1, 2 </details> | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| conversationId | STRING | TEXT |
| message | STRING | TEXT |
| attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "New Chat Request",
  "name" : "newChatRequest",
  "parameters" : {
    "mode" : 1
  },
  "type" : "chat/v1/newChatRequest"
}
```


<hr />

