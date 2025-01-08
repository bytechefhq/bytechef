---
title: "Chat"
description: "Actions and triggers for using with the chat widget."
---
## Reference
<hr />

Actions and triggers for using with the chat widget.


Categories: [helpers]


Version: 1

<hr />




## Triggers


### New Chat Request
.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| INTEGER | SELECT  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| [FILE_ENTRY] | ARRAY_BUILDER  |







<hr />



## Actions


### Response to Chat Request
Converts the response to chat request.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message | STRING | TEXT  |  The message of the response.  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  The attachments of the response.  |




