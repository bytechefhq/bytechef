---
title: "Infobip"
description: "Infobip is a global communications platform that provide cloud-based messaging and omnichannel communication solutions for businesses."
---
## Reference
<hr />

Infobip is a global communications platform that provide cloud-based messaging and omnichannel communication solutions for businesses.


Categories: [COMMUNICATION]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| API Key | STRING | TEXT  |  |
| Base URL | STRING | TEXT  |  Personalized base URL for API requests.  |





<hr />





## Actions


### Send SMS
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | TEXT  |  The sender ID. It can be alphanumeric or numeric (e.g., CompanyName).  |
| To | [STRING] | ARRAY_BUILDER  |  Message recipient numbers.  |
| Text | STRING | TEXT  |  Content of the message being sent.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| [STRING\($messageId), {INTEGER\(groupId), STRING\(groupName), INTEGER\(id), STRING\(name), STRING\(description)}\($status), STRING\($to), INTEGER\($smsCount)] | ARRAY_BUILDER  |






### Send Whatsapp Text Message
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | TEXT  |  Registered WhatsApp sender number. Must be in international format and comply with WhatsApp's requirements.  |
| To | STRING | TEXT  |  Message recipient number. Must be in international format.  |
| Text | STRING | TEXT  |  Content of the message being sent.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {INTEGER\(groupId), STRING\(groupName), INTEGER\(id), STRING\(name), STRING\(description)} | OBJECT_BUILDER  |






<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)"><iframe src="https://www.guidejar.com/embed/7e252985-dce7-48b9-bf79-50e81568ca22?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
