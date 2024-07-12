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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />





## Actions


### Send message
Posts a message to a public channel, private channel, or existing direct message conversation.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Channel | STRING | SELECT  |
| Content type | STRING | SELECT  |
| Attachments | STRING | TEXT  |
| Blocks | STRING | TEXT  |
| Text | STRING | TEXT  |
| As user | BOOLEAN | SELECT  |
| Icon emoji | STRING | TEXT  |
| Icon URL | STRING | TEXT  |
| Link names | BOOLEAN | SELECT  |
| Metadata | STRING | TEXT  |
| Mrkdwn | BOOLEAN | SELECT  |
| Parse | STRING | TEXT  |
| Reply broadcast | BOOLEAN | SELECT  |
| Thread ts | STRING | TEXT  |
| Unfurl links | BOOLEAN | SELECT  |
| Unfurl media | BOOLEAN | SELECT  |
| Username | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Send direct message
Sends a direct message to another user in a workspace. If it hasn't already, a direct message conversation will be created.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| User | STRING | SELECT  |
| Content type | STRING | SELECT  |
| Attachments | STRING | TEXT  |
| Blocks | STRING | TEXT  |
| Text | STRING | TEXT  |
| As user | BOOLEAN | SELECT  |
| Icon emoji | STRING | TEXT  |
| Icon URL | STRING | TEXT  |
| Link names | BOOLEAN | SELECT  |
| Metadata | STRING | TEXT  |
| Mrkdwn | BOOLEAN | SELECT  |
| Parse | STRING | TEXT  |
| Reply broadcast | BOOLEAN | SELECT  |
| Thread ts | STRING | TEXT  |
| Unfurl links | BOOLEAN | SELECT  |
| Unfurl media | BOOLEAN | SELECT  |
| Username | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





