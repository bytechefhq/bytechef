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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |





<hr />





## Actions


### Send SMS
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Bulk ID | STRING | TEXT  |
| Messages | ARRAY | ARRAY_BUILDER  |
| Sending speed limit | OBJECT | OBJECT_BUILDER  |
| Url options | OBJECT | OBJECT_BUILDER  |
| Tracking | OBJECT | OBJECT_BUILDER  |
| Include SMS count in response | BOOLEAN | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |





### Send Whatsapp Text Message
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From | STRING | TEXT  |
| To | STRING | TEXT  |
| Message ID | STRING | TEXT  |
| Content | OBJECT | OBJECT_BUILDER  |
| Callback data | STRING | TEXT  |
| Notify URL | STRING | TEXT  |
| URL options | OBJECT | OBJECT_BUILDER  |
| Entity ID | STRING | TEXT  |
| Application ID | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





