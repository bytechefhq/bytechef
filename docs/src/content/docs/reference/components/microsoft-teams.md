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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |
| Tenant Id | STRING | TEXT  |





<hr />





## Actions


### Create channel
Creates a new channel within a team.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Team | STRING | SELECT  |
| Channel name | STRING | TEXT  |
| Description | STRING | TEXT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Team | STRING | SELECT  |
| Channel to send message to. | STRING | SELECT  |
| Message text format | STRING | SELECT  |
| Message text | STRING | TEXT_AREA  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





### Send chat message
Sends a message in an existing chat.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Chat | STRING | SELECT  |
| Message text format | STRING | SELECT  |
| Message text | STRING | TEXT_AREA  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





