---
title: "Slack"
description: "Slack is a messaging platform for teams to communicate and collaborate."
---
## Reference
<hr />

Slack is a messaging platform for teams to communicate and collaborate.


Categories: [COMMUNICATION, DEVELOPER_TOOLS]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />





## Actions


### Send message
Posts a message to a public channel, private channel, or existing direct message conversation.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Channel | STRING | SELECT  |  The id of a channel where the message will be sent.  |
| Content type | STRING | SELECT  |  One of these arguments is required to describe the content of the message. If attachments or blocks are included, text will be used as fallback text for notifications only.  |
| Attachments | STRING | TEXT  |  A JSON-based array of structured attachments, presented as a URL-encoded string.  |
| Blocks | STRING | TEXT  |  A JSON-based array of structured blocks, presented as a URL-encoded string.  |
| Text | STRING | TEXT  |  How this field works and whether it is required depends on other fields you use in your API call.  |
| As user | BOOLEAN | SELECT  |  (Legacy) Pass true to post the message as the authed user instead of as a bot. Defaults to false. Can only be used by classic Slack apps. See authorship below.  |
| Icon emoji | STRING | TEXT  |  Emoji to use as the icon for this message. Overrides icon_url.  |
| Icon URL | STRING | TEXT  |  URL to an image to use as the icon for this message.  |
| Link names | BOOLEAN | SELECT  |  Find and link user groups. No longer supports linking individual users; use syntax shown in Mentioning Users instead.  |
| Metadata | STRING | TEXT  |  JSON object with event_type and event_payload fields, presented as a URL-encoded string. Metadata you post to Slack is accessible to any app or user who is a member of that workspace.  |
| Mrkdwn | BOOLEAN | SELECT  |  Disable Slack markup parsing by setting to false. Enabled by default.  |
| Parse | STRING | TEXT  |  Change how messages are treated. See below.  |
| Reply broadcast | BOOLEAN | SELECT  |  Used in conjunction with thread_ts and indicates whether reply should be made visible to everyone in the channel or conversation. Defaults to false.  |
| Thread ts | STRING | TEXT  |  Provide another message's ts value to make this message a reply. Avoid using a reply's ts value; use its parent instead.  |
| Unfurl links | BOOLEAN | SELECT  |  Pass true to enable unfurling of primarily text-based content.  |
| Unfurl media | BOOLEAN | SELECT  |  Pass false to disable unfurling of media content.  |
| Username | STRING | TEXT  |  Set your bot's user name.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [] | ARRAY_BUILDER  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |
| {[STRING](messages)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(type), STRING(subtype), STRING(team), STRING(channel), STRING(user), STRING(username), STRING(text), STRING(ts), STRING(threadTs)} | OBJECT_BUILDER  |





### Send direct message
Sends a direct message to another user in a workspace. If it hasn't already, a direct message conversation will be created.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| User | STRING | SELECT  |  The id of a user to send the direct message to.  |
| Content type | STRING | SELECT  |  One of these arguments is required to describe the content of the message. If attachments or blocks are included, text will be used as fallback text for notifications only.  |
| Attachments | STRING | TEXT  |  A JSON-based array of structured attachments, presented as a URL-encoded string.  |
| Blocks | STRING | TEXT  |  A JSON-based array of structured blocks, presented as a URL-encoded string.  |
| Text | STRING | TEXT  |  How this field works and whether it is required depends on other fields you use in your API call.  |
| As user | BOOLEAN | SELECT  |  (Legacy) Pass true to post the message as the authed user instead of as a bot. Defaults to false. Can only be used by classic Slack apps. See authorship below.  |
| Icon emoji | STRING | TEXT  |  Emoji to use as the icon for this message. Overrides icon_url.  |
| Icon URL | STRING | TEXT  |  URL to an image to use as the icon for this message.  |
| Link names | BOOLEAN | SELECT  |  Find and link user groups. No longer supports linking individual users; use syntax shown in Mentioning Users instead.  |
| Metadata | STRING | TEXT  |  JSON object with event_type and event_payload fields, presented as a URL-encoded string. Metadata you post to Slack is accessible to any app or user who is a member of that workspace.  |
| Mrkdwn | BOOLEAN | SELECT  |  Disable Slack markup parsing by setting to false. Enabled by default.  |
| Parse | STRING | TEXT  |  Change how messages are treated. See below.  |
| Reply broadcast | BOOLEAN | SELECT  |  Used in conjunction with thread_ts and indicates whether reply should be made visible to everyone in the channel or conversation. Defaults to false.  |
| Thread ts | STRING | TEXT  |  Provide another message's ts value to make this message a reply. Avoid using a reply's ts value; use its parent instead.  |
| Unfurl links | BOOLEAN | SELECT  |  Pass true to enable unfurling of primarily text-based content.  |
| Unfurl media | BOOLEAN | SELECT  |  Pass false to disable unfurling of media content.  |
| Username | STRING | TEXT  |  Set your bot's user name.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [] | ARRAY_BUILDER  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |
| {[STRING](messages)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(type), STRING(subtype), STRING(team), STRING(channel), STRING(user), STRING(username), STRING(text), STRING(ts), STRING(threadTs)} | OBJECT_BUILDER  |





