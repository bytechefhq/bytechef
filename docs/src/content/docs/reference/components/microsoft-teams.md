---
title: "Microsoft Teams"
description: "Microsoft Teams is a collaboration platform that combines workplace chat, video meetings, file storage, and application integration."
---
## Reference
<hr />

Microsoft Teams is a collaboration platform that combines workplace chat, video meetings, file storage, and application integration.


Categories: [COMMUNICATION]


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
| Tenant Id | STRING | TEXT  |  |





<hr />





## Actions


### Create channel
Creates a new channel within a team.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Team | STRING | SELECT  |  Team where the channel will be created.  |
| Channel name | STRING | TEXT  |  |
| Description | STRING | TEXT  |  Description for the channel.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Send channel message
Sends a message to a channel.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Team | STRING | SELECT  |  Team where the channel is located.  |
| Channel to send message to. | STRING | SELECT  |  |
| Message text format | STRING | SELECT  |  |
| Message text | STRING | TEXT_AREA  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| {STRING(contentType), STRING(content)} | OBJECT_BUILDER  |
| {STRING(teamId), STRING(channelId)} | OBJECT_BUILDER  |





### Send chat message
Sends a message in an existing chat.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Chat | STRING | SELECT  |  |
| Message text format | STRING | SELECT  |  |
| Message text | STRING | TEXT_AREA  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(contentType), STRING(content)} | OBJECT_BUILDER  |





