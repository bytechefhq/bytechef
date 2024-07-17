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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Bot token | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Send channel message
Post a new message to a specific #channel you choose.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Guild | STRING | SELECT  |  |
| Channel | STRING | SELECT  |  Channel where to send the message  |
| Message | {STRING(content), BOOLEAN(tts)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(content), BOOLEAN(tts)} | OBJECT_BUILDER  |





### Create channel
Create a new channel

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Guild | STRING | SELECT  |  |
| Channel | {STRING(name), INTEGER(type)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), INTEGER(type), STRING(name)} | OBJECT_BUILDER  |





### Send direct message
Send direct message guild member

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Guild | STRING | SELECT  |  |
| Recipient | STRING | SELECT  |  The recipient to open a DM channel with  |
| Message Text | STRING | TEXT  |  Message contents (up to 2000 characters)  |
| Text To Speech | BOOLEAN | SELECT  |  True if this is a TTS message  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id)} | OBJECT_BUILDER  |





<hr />

# Additional instructions
<hr />

![anl-c-discord-md](https://static.scarf.sh/a.png?x-pxid=8dad9aeb-34e5-47b6-917f-5423fe8d2b0c)
## CONNECTION

[Setting up OAuth2](https://discordjs.guide/preparations/adding-your-bot-to-servers.html#bot-invite-links)

[Guidejar](https://guidejar.com/guides/31087152-2446-4f70-a391-79f49c45190a) tutorial.
