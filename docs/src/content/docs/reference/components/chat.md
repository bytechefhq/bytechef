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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| message | Message | STRING | The message of the response. | null |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | The attachments of the response. | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| mode | | INTEGER <details> <summary> Options </summary> 1, 2 </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| conversationId | STRING |  |
| message | STRING |  |
| attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> |  |




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

