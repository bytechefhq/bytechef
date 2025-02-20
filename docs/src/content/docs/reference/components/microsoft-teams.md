---
title: "Microsoft Teams"
description: "Microsoft Teams is a collaboration platform that combines workplace chat, video meetings, file storage, and application integration."
---

Microsoft Teams is a collaboration platform that combines workplace chat, video meetings, file storage, and application integration.


Categories: communication


Type: microsoftTeams/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| tenantId | Tenant Id | STRING | TEXT |  | true |





<hr />



## Actions


### Create Channel
Name: createChannel

Creates a new channel within a team.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| teamId | Team ID | STRING | SELECT | ID of the team where the channel will be created. | true |
| displayName | Channel Name | STRING | TEXT |  | true |
| description | Description | STRING | TEXT | Description for the channel. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| displayName | STRING | TEXT |
| description | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Channel",
  "name" : "createChannel",
  "parameters" : {
    "teamId" : "",
    "displayName" : "",
    "description" : ""
  },
  "type" : "microsoftTeams/v1/createChannel"
}
```


### Send Channel Message
Name: sendChannelMessage

Sends a message to a channel.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| teamId | Team ID | STRING | SELECT | ID of the team where the channel is located. | true |
| channelId | Channel ID | STRING <details> <summary> Depends On </summary> teamId </details> | SELECT | Channel to send message to. | true |
| contentType | Message Text Format | STRING <details> <summary> Options </summary> text, html </details> | SELECT |  | true |
| content | Message Text | STRING | TEXT_AREA |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(contentType), STRING\(content)} </details> | OBJECT_BUILDER |
| channelIdentity | OBJECT <details> <summary> Properties </summary> {STRING\(teamId), STRING\(channelId)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Send Channel Message",
  "name" : "sendChannelMessage",
  "parameters" : {
    "teamId" : "",
    "channelId" : "",
    "contentType" : "",
    "content" : ""
  },
  "type" : "microsoftTeams/v1/sendChannelMessage"
}
```


### Send Chat Message
Name: sendChatMessage

Sends a message in an existing chat.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| chatId | Chat ID | STRING | SELECT |  | true |
| contentType | Message Text Format | STRING <details> <summary> Options </summary> text, html </details> | SELECT |  | true |
| content | Message Text | STRING | TEXT_AREA |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| chatId | STRING | TEXT |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(contentType), STRING\(content)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Send Chat Message",
  "name" : "sendChatMessage",
  "parameters" : {
    "chatId" : "",
    "contentType" : "",
    "content" : ""
  },
  "type" : "microsoftTeams/v1/sendChatMessage"
}
```




