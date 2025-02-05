---
title: "Infobip"
description: "Infobip is a global communications platform that provide cloud-based messaging and omnichannel communication solutions for businesses."
---

Infobip is a global communications platform that provide cloud-based messaging and omnichannel communication solutions for businesses.


Categories: communication


Type: infobip/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | API Key | STRING | TEXT  |  | true  |
| baseUrl | Base URL | STRING | TEXT  |  Personalized base URL for API requests.  |  true  |





<hr />



## Actions


### Send SMS
Send a new SMS message

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| sender | From | STRING | TEXT  |  The sender ID. It can be alphanumeric or numeric (e.g., CompanyName).  |  true  |
| to | To | [STRING] | ARRAY_BUILDER  |  Message recipient numbers.  |  true  |
| text | Text | STRING | TEXT  |  Content of the message being sent.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| bulkId | STRING | TEXT  |
| messages | [STRING\($messageId), {INTEGER\(groupId), STRING\(groupName), INTEGER\(id), STRING\(name), STRING\(description)}\($status), STRING\($to), INTEGER\($smsCount)] | ARRAY_BUILDER  |






### Send Whatsapp Text Message
Send a new SMS message

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from | From | STRING | TEXT  |  Registered WhatsApp sender number. Must be in international format and comply with WhatsApp's requirements.  |  true  |
| to | To | STRING | TEXT  |  Message recipient number. Must be in international format.  |  true  |
| text | Text | STRING | TEXT  |  Content of the message being sent.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| to | STRING | TEXT  |
| messageCount | INTEGER | INTEGER  |
| messageId | STRING | TEXT  |
| status | {INTEGER\(groupId), STRING\(groupName), INTEGER\(id), STRING\(name), STRING\(description)} | OBJECT_BUILDER  |








<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)"><iframe src="https://www.guidejar.com/embed/7e252985-dce7-48b9-bf79-50e81568ca22?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
