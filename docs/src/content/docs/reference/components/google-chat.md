---
title: "Google Chat"
description: "Google Chat is an intelligent and secure communication and collaboration tool, built for teams."
---

Google Chat is an intelligent and secure communication and collaboration tool, built for teams.


Categories: Helpers


Type: googleChat/v1

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


### Create Message
Name: createMessage

Creates a new message in selected space.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| SPACE | | STRING |  | null |
| messageText | Message Text | STRING | Text of the message. | true |

#### Example JSON Structure
```json
{
  "label" : "Create Message",
  "name" : "createMessage",
  "parameters" : {
    "SPACE" : "",
    "messageText" : ""
  },
  "type" : "googleChat/v1/createMessage"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| name | STRING | Name of the message that was created. |
| sender | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(type)} </details> | Sender of the message. |
| createTime | STRING | Time when the message was created. |
| text | STRING | Text of the message. |
| thread | OBJECT <details> <summary> Properties </summary> {STRING\(name)} </details> |  |
| space | OBJECT <details> <summary> Properties </summary> {STRING\(name)} </details> |  |
| argumentText | STRING |  |
| formattedText | STRING |  |




#### Output Example
```json
{
  "name" : "",
  "sender" : {
    "name" : "",
    "type" : ""
  },
  "createTime" : "",
  "text" : "",
  "thread" : {
    "name" : ""
  },
  "space" : {
    "name" : ""
  },
  "argumentText" : "",
  "formattedText" : ""
}
```


### Create Space
Name: createSpace

Creates space in Google Chat.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| displayName | Space Name | STRING | Name of the space to create. | true |

#### Example JSON Structure
```json
{
  "label" : "Create Space",
  "name" : "createSpace",
  "parameters" : {
    "displayName" : ""
  },
  "type" : "googleChat/v1/createSpace"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| name | STRING | Name of the space that was created. |
| type | STRING | Type of the space. |
| displayName | STRING | Name of the space that will be displayed. |
| spaceThreadingState | STRING |  |
| spaceType | STRING |  |
| spaceHistoryState | STRING |  |
| createTime | STRING |  |
| lastActiveTime | STRING |  |
| membershipCount | OBJECT <details> <summary> Properties </summary> {} </details> |  |
| accessSettings | OBJECT <details> <summary> Properties </summary> {STRING\(accessSettings)} </details> |  |
| spaceUri | STRING |  |




#### Output Example
```json
{
  "name" : "",
  "type" : "",
  "displayName" : "",
  "spaceThreadingState" : "",
  "spaceType" : "",
  "spaceHistoryState" : "",
  "createTime" : "",
  "lastActiveTime" : "",
  "membershipCount" : { },
  "accessSettings" : {
    "accessSettings" : ""
  },
  "spaceUri" : ""
}
```




<hr />

# Additional instructions
<hr />

After enabling Chat API in Google API Console make sure you also follow these steps:
https://developers.google.com/workspace/chat/configure-chat-api
