---
title: "Discord"
description: "Discord is a communication platform designed for creating communities, chatting with friends, and connecting with others through text, voice, and video channels."
---
## Reference
<hr />

Discord is a communication platform designed for creating communities, chatting with friends, and connecting with others through text, voice, and video channels.

Categories: [COMMUNICATION]

Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Bot token | STRING | TEXT  |





<hr />



## Triggers



<hr />



## Actions


### Send channel message
Post a new message to a specific #channel you choose.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Guild | STRING | SELECT  |
| Channel | STRING | SELECT  |
| Message | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Create channel
Create a new channel

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Guild | STRING | SELECT  |
| Channel | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





### Send direct message
Send direct message guild member

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Guild | STRING | SELECT  |
| Recipient | STRING | SELECT  |
| Message Text | STRING | TEXT  |
| Text To Speech | BOOLEAN | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





