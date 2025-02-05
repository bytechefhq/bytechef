---
title: "Discord"
description: "Discord is a communication platform designed for creating communities, chatting with friends, and connecting with others through text, voice, and video channels."
---

Discord is a communication platform designed for creating communities, chatting with friends, and connecting with others through text, voice, and video channels.


Categories: communication


Type: discord/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Bot token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Send Channel Message
Post a new message to a specific #channel you choose.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| guildId | Guild ID | STRING | SELECT  |  | true  |
| channelId | Channel ID | STRING | SELECT  |  ID of the channel where to send the message.  |  true  |
| __item | Message | {STRING\(content), BOOLEAN\(tts)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(content), BOOLEAN\(tts)} | OBJECT_BUILDER  |






### Create Channel
Create a new channel

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| guildId | Guild ID | STRING | SELECT  |  | true  |
| __item | Channel | {STRING\(name), INTEGER\(type)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), INTEGER\(type), STRING\(name)} | OBJECT_BUILDER  |






### Send Direct Message
Send direct message guild member.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| guildId | Guild ID | STRING | SELECT  |  | true  |
| recipient_id | Recipient | STRING | SELECT  |  The recipient to open a DM channel with.  |  true  |
| content | Message Text | STRING | TEXT  |  Message contents (up to 2000 characters)  |  true  |
| tts | Text to Speech | BOOLEAN | SELECT  |  True if this is a TTS message  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id)} | OBJECT_BUILDER  |








## Triggers



<hr />

<hr />

# Additional instructions
<hr />

![anl-c-discord-md](https://static.scarf.sh/a.png?x-pxid=8dad9aeb-34e5-47b6-917f-5423fe8d2b0c)
## CONNECTION

[Setting up OAuth2](https://discordjs.guide/preparations/adding-your-bot-to-servers.html#bot-invite-links)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.69531250% + 32px)"><iframe src="https://www.guidejar.com/embed/31087152-2446-4f70-a391-79f49c45190a?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
