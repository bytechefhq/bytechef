---
title: "Twilio"
description: "Twilio is a cloud communications platform that enables developers to integrate messaging, voice, and video capabilities into their applications."
---

Twilio is a cloud communications platform that enables developers to integrate messaging, voice, and video capabilities into their applications.


Categories: communication


Type: twilio/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Account SID | STRING | TEXT  |  The Account SID from your Twilio account.  |  true  |
| password | Auth Token | STRING | TEXT  |  The Auth Token from your Twilio account.  |  true  |





<hr />



## Actions


### Send SMS
Name: sendSMS

Send a new SMS message

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| To | To | STRING | PHONE  |  The recipient's phone number in E.164 format (for SMS/MMS) or channel address, e.g. whatsapp:+15552229999.  |  true  |
| From | From | STRING | PHONE  |  The sender's Twilio phone number (in E.164 format), alphanumeric sender ID, Wireless SIM, short code, or channel address (e.g., whatsapp:+15554449999). The value of the from parameter must be a sender that is hosted within Twilio and belongs to the Account creating the Message. If you are using messaging_service_sid, this parameter can be empty (Twilio assigns a from value from the Messaging Service's Sender Pool) or you can provide a specific sender from your Sender Pool.  |  true  |
| Body | Body | STRING | TEXT  |  The text content of the outgoing message. SMS only: If the body contains more than 160 GSM-7 characters (or 70 UCS-2 characters), the message is segmented and charged accordingly. For long body text, consider using the send_as_mms parameter.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | STRING | TEXT  |
| numSegments | STRING | TEXT  |
| direction | STRING | TEXT  |
| from | {STRING\(rawNumber)} | OBJECT_BUILDER  |
| to | STRING | TEXT  |
| dateUpdated | {DATE_TIME\(dateTime), STRING\(zoneId)} | OBJECT_BUILDER  |
| price | STRING | TEXT  |
| errorMessage | STRING | TEXT  |
| uri | STRING | TEXT  |
| accountSid | STRING | TEXT  |
| numMedia | STRING | TEXT  |
| status | STRING | TEXT  |
| messagingServiceSid | STRING | TEXT  |
| sid | STRING | TEXT  |
| dateSent | {DATE_TIME\(dateTime), STRING\(zoneId)} | OBJECT_BUILDER  |
| dateCreated | {DATE_TIME\(dateTime), STRING\(zoneId)} | OBJECT_BUILDER  |
| errorCode | INTEGER | INTEGER  |
| currency | {STRING\(currencyCode), INTEGER\(defaultFractionDigits), INTEGER\(numericCode)} | OBJECT_BUILDER  |
| apiVersion | STRING | TEXT  |
| subresourceUris | {} | OBJECT_BUILDER  |








