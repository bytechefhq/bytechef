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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Send message
Name: sendMessage

Sends a message to a public channel, private channel, or existing direct message conversation.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| channel | Channel ID | STRING | SELECT | ID of the channel, private group, or IM channel to send message to. | true |
| text | Message | STRING | TEXT_AREA | The text of your message. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| ok | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| channel | STRING | TEXT |
| ts | STRING | TEXT |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(user), STRING\(type), STRING\(ts), STRING\(text), STRING\(team), STRING\(subtype)} </details> | OBJECT_BUILDER |
| warning | STRING | TEXT |
| responseMetadata | OBJECT <details> <summary> Properties </summary> {[STRING]\(messages)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| channel | User ID | STRING | SELECT | ID of the user to send the direct message to. | true |
| text | Message | STRING | TEXT_AREA | The text of your message. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| ok | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| channel | STRING | TEXT |
| ts | STRING | TEXT |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(user), STRING\(type), STRING\(ts), STRING\(text), STRING\(team), STRING\(subtype)} </details> | OBJECT_BUILDER |
| warning | STRING | TEXT |
| responseMetadata | OBJECT <details> <summary> Properties </summary> {[STRING]\(messages)} </details> | OBJECT_BUILDER |




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




