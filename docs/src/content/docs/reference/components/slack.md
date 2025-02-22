---
title: "Slack"
description: "Slack is a messaging platform for teams to communicate and collaborate."
---

Slack is a messaging platform for teams to communicate and collaborate.


Categories: communication, developer-tools


Type: slack/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Request Approval in a Channel
Name: requestApprovalMessage

Send approval message to a channel and then wait until the message is approved or disapproved.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| channel | Channel | STRING | Channel, private group, or IM channel to send message to. | true |
| text | Message | STRING | The text of your message. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| ok | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| channel | STRING |
| ts | STRING |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(user), STRING\(type), STRING\(ts), STRING\(text), STRING\(team), STRING\(subtype)} </details> |
| warning | STRING |
| responseMetadata | OBJECT <details> <summary> Properties </summary> {[STRING]\(messages)} </details> |




#### JSON Example
```json
{
  "label" : "Request Approval in a Channel",
  "name" : "requestApprovalMessage",
  "parameters" : {
    "channel" : "",
    "text" : ""
  },
  "type" : "slack/v1/requestApprovalMessage"
}
```


### Send message
Name: sendMessage

Sends a message to a public channel, private channel, or existing direct message conversation.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| channel | Channel ID | STRING | ID of the channel, private group, or IM channel to send message to. | true |
| text | Message | STRING | The text of your message. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| ok | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| channel | STRING |
| ts | STRING |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(user), STRING\(type), STRING\(ts), STRING\(text), STRING\(team), STRING\(subtype)} </details> |
| warning | STRING |
| responseMetadata | OBJECT <details> <summary> Properties </summary> {[STRING]\(messages)} </details> |




#### JSON Example
```json
{
  "label" : "Send message",
  "name" : "sendMessage",
  "parameters" : {
    "channel" : "",
    "text" : ""
  },
  "type" : "slack/v1/sendMessage"
}
```


### Send Direct Message
Name: sendDirectMessage

Sends a direct message to another user in a workspace. If it hasn't already, a direct message conversation will be created.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| channel | User ID | STRING | ID of the user to send the direct message to. | true |
| text | Message | STRING | The text of your message. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| ok | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| channel | STRING |
| ts | STRING |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(user), STRING\(type), STRING\(ts), STRING\(text), STRING\(team), STRING\(subtype)} </details> |
| warning | STRING |
| responseMetadata | OBJECT <details> <summary> Properties </summary> {[STRING]\(messages)} </details> |




#### JSON Example
```json
{
  "label" : "Send Direct Message",
  "name" : "sendDirectMessage",
  "parameters" : {
    "channel" : "",
    "text" : ""
  },
  "type" : "slack/v1/sendDirectMessage"
}
```




