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
| Token | STRING | TEXT  |  |





<hr />





## Actions


### Send SMS
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Bulk ID | STRING | TEXT  |  Unique ID assigned to the request if messaging multiple recipients or sending multiple messages via a single API request. If not provided, it will be auto-generated and returned in the API response. Typically, used to fetch delivery reports and message logs.  |
| Messages | [{STRING(callbackData), {[STRING](days), {INTEGER(hour), INTEGER(minute)}(from), {INTEGER(hour), INTEGER(minute)}(to)}(deliveryTimeWindow), [{STRING(messageId), STRING(to)}](destinations), BOOLEAN(flash), STRING(from), BOOLEAN(intermediateReport), {STRING(languageCode)}(language), STRING(notifyContentType), STRING(notifyUrl), {{STRING(contentTemplateId), STRING(principalEntityId)}(indiaDlt), {INTEGER(brandCode), STRING(recipientType)}(turkeyIys), {INTEGER(resellerCode)}(southKorea)}(regional), {DATE_TIME(dateTime), STRING(zoneId)}(sendAt), STRING(text), STRING(transliteration), NUMBER(validityPeriod), STRING(entityId), STRING(applicationId)}] | ARRAY_BUILDER  |  An array of message objects of a single message or multiple messages sent under one bulk ID.  |
| Sending speed limit | {INTEGER(amount), STRING(timeUnit)} | OBJECT_BUILDER  |  Limits the send speed when sending messages in bulk to deliver messages over a longer period of time. You may wish to use this to allow your systems or agents to handle large amounts of incoming traffic, e.g., if you are expecting recipients to follow through with a call-to-action option from a message you sent. Not setting a send speed limit can overwhelm your resources with incoming traffic.  |
| Url options | {BOOLEAN(shortenUrl), BOOLEAN(trackClicks), STRING(trackingUrl), BOOLEAN(removeProtocol), STRING(customDomain)} | OBJECT_BUILDER  |  Sets up URL shortening and tracking feature. Not compatible with old tracking feature.  |
| Tracking | {STRING(baseUrl), STRING(processKey), STRING(track), STRING(type)} | OBJECT_BUILDER  |  Sets up tracking parameters to track conversion metrics and type.  |
| Include SMS count in response | BOOLEAN | SELECT  |  Set to true to return smsCount in the response. smsCount is the total count of SMS submitted in the request. SMS messages have a character limit and messages longer than that limit will be split into multiple SMS and reflected in the total count of SMS submitted.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| [STRING($messageId), {STRING(groupName), INTEGER(id), INTEGER(groupId), STRING(name), STRING(action), STRING(description)}($status), STRING($to), INTEGER($smsCount)] | ARRAY_BUILDER  |





### Send Whatsapp Text Message
Send a new SMS message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | TEXT  |  Registered WhatsApp sender number. Must be in international format and comply with WhatsApp's requirements.  |
| To | STRING | TEXT  |  Message recipient number. Must be in international format.  |
| Message ID | STRING | TEXT  |  The ID that uniquely identifies the message sent.  |
| Content | {STRING(text), BOOLEAN(previewUrl)} | OBJECT_BUILDER  |  The content object to build a message that will be sent.  |
| Callback data | STRING | TEXT  |  Custom client data that will be included in a Delivery Report.  |
| Notify URL | STRING | TEXT  |  The URL on your callback server to which delivery and seen reports will be sent.  |
| URL options | {BOOLEAN(shortenUrl), BOOLEAN(trackClicks), STRING(trackingUrl), BOOLEAN(removeProtocol), STRING(customDomain)} | OBJECT_BUILDER  |  Sets up URL shortening and tracking feature. Not compatible with old tracking feature.  |
| Entity ID | STRING | TEXT  |  Required for entity use in a send request for outbound traffic. Returned in notification events.  |
| Application ID | STRING | TEXT  |  Required for application use in a send request for outbound traffic. Returned in notification events.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {INTEGER(groupId), STRING(groupName), INTEGER(id), STRING(name), STRING(description), STRING(action)} | OBJECT_BUILDER  |





