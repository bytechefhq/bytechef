---
title: "Rocketchat"
description: "Rocket.Chat is a communication platform that enables team collaboration through messaging, audio/video calls, and integrations, all customizable and self-hostable."
---

Rocket.Chat is a communication platform that enables team collaboration through messaging, audio/video calls, and integrations, all customizable and self-hostable.


Categories: Communication


Type: rocketchat/v1

<hr />



## Connections

Version: 1


### custom

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| domain | Domain | STRING |  | true |
| X-Auth-Token | Auth Token | STRING |  | true |
| X-User-Id | User ID | STRING |  | true |





<hr />



## Actions


### Send Direct Message
Name: sendDirectMessage

Send messages to users on your workspace.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING | Username to send the direct message to. | true |
| text | Message | STRING | The message to send. | true |

#### Example JSON Structure
```json
{
  "label" : "Send Direct Message",
  "name" : "sendDirectMessage",
  "parameters" : {
    "username" : "",
    "text" : ""
  },
  "type" : "rocketchat/v1/sendDirectMessage"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| ts | INTEGER |  |
| channel | STRING |  |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(alias), STRING\(msg), []\(attachments), BOOLEAN\(parseUrls), BOOLEAN\(groupable), STRING\(ts), {STRING\(_id), STRING\(username), STRING\(name)}\(u), STRING\(rid), STRING\(_id), STRING\(_updateAt), []\(urls), []\(mentions), []\(channels), []\(md)} </details> |  |
| success | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |




#### Output Example
```json
{
  "ts" : 1,
  "channel" : "",
  "message" : {
    "alias" : "",
    "msg" : "",
    "attachments" : [ ],
    "parseUrls" : false,
    "groupable" : false,
    "ts" : "",
    "u" : {
      "_id" : "",
      "username" : "",
      "name" : ""
    },
    "rid" : "",
    "_id" : "",
    "_updateAt" : "",
    "urls" : [ ],
    "mentions" : [ ],
    "channels" : [ ],
    "md" : [ ]
  },
  "success" : false
}
```


### Send Channel Message
Name: sendChannelMessage

Send messages to channel on your workspace.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Channel Name | STRING | Channel name to send the message to. | true |
| text | Message | STRING | The message to send. | true |

#### Example JSON Structure
```json
{
  "label" : "Send Channel Message",
  "name" : "sendChannelMessage",
  "parameters" : {
    "name" : "",
    "text" : ""
  },
  "type" : "rocketchat/v1/sendChannelMessage"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| ts | INTEGER |  |
| channel | STRING |  |
| message | OBJECT <details> <summary> Properties </summary> {STRING\(alias), STRING\(msg), []\(attachments), BOOLEAN\(parseUrls), BOOLEAN\(groupable), STRING\(ts), {STRING\(_id), STRING\(username), STRING\(name)}\(u), STRING\(rid), STRING\(_id), STRING\(_updateAt), []\(urls), []\(mentions), []\(channels), []\(md)} </details> |  |
| success | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |




#### Output Example
```json
{
  "ts" : 1,
  "channel" : "",
  "message" : {
    "alias" : "",
    "msg" : "",
    "attachments" : [ ],
    "parseUrls" : false,
    "groupable" : false,
    "ts" : "",
    "u" : {
      "_id" : "",
      "username" : "",
      "name" : ""
    },
    "rid" : "",
    "_id" : "",
    "_updateAt" : "",
    "urls" : [ ],
    "mentions" : [ ],
    "channels" : [ ],
    "md" : [ ]
  },
  "success" : false
}
```


### Create Channel
Name: createChannel

Create a public channel.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Channel Name | STRING | The name of the channel. | true |
| members | Members | ARRAY <details> <summary> Items </summary> [STRING] </details> | An array of the users to be added to the channel when it is created. | false |
| readOnly | Read Only | BOOLEAN <details> <summary> Options </summary> true, false </details> | Set if the channel is read only or not. | false |
| excludeSelf | Exclude Self | BOOLEAN <details> <summary> Options </summary> true, false </details> | If set to true, the user calling the endpoint is not automatically added as a member of the channel. | false |

#### Example JSON Structure
```json
{
  "label" : "Create Channel",
  "name" : "createChannel",
  "parameters" : {
    "name" : "",
    "members" : [ "" ],
    "readOnly" : false,
    "excludeSelf" : false
  },
  "type" : "rocketchat/v1/createChannel"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| channel | OBJECT <details> <summary> Properties </summary> {STRING\(_id), STRING\(fname), STRING\(_updateAt), {}\(customFields), STRING\(name), STRING\(t), INTEGER\(msgs), INTEGER\(usersCount), {STRING\(_id), STRING\(username), STRING\(name)}\(u), STRING\(ts), BOOLEAN\(ro), BOOLEAN\(default), BOOLEAN\(sysMes)} </details> |  |
| success | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |




#### Output Example
```json
{
  "channel" : {
    "_id" : "",
    "fname" : "",
    "_updateAt" : "",
    "customFields" : { },
    "name" : "",
    "t" : "",
    "msgs" : 1,
    "usersCount" : 1,
    "u" : {
      "_id" : "",
      "username" : "",
      "name" : ""
    },
    "ts" : "",
    "ro" : false,
    "default" : false,
    "sysMes" : false
  },
  "success" : false
}
```




## Triggers


### New Message
Name: newMessage

Trigger off whenever a new message is posted to any public channel, private group or direct messages.

Type: DYNAMIC_WEBHOOK


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "New Message",
  "name" : "newMessage",
  "type" : "rocketchat/v1/newMessage"
}
```


<hr />

