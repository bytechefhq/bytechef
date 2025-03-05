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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Account SID | STRING | The Account SID from your Twilio account. | true |
| password | Auth Token | STRING | The Auth Token from your Twilio account. | true |





<hr />



## Actions


### Send SMS
Name: sendSMS

Send a new SMS message

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| To | To | STRING | The recipient's phone number in E.164 format (for SMS/MMS) or channel address, e.g. whatsapp:+15552229999. | true |
| From | From | STRING | The sender's Twilio phone number (in E.164 format), alphanumeric sender ID, Wireless SIM, short code, or channel address (e.g., whatsapp:+15554449999). The value of the from parameter must be a sender that is hosted within Twilio and belongs to the Account creating the Message. If you are using messaging_service_sid, this parameter can be empty (Twilio assigns a from value from the Messaging Service's Sender Pool) or you can provide a specific sender from your Sender Pool. | true |
| Body | Body | STRING | The text content of the outgoing message. SMS only: If the body contains more than 160 GSM-7 characters (or 70 UCS-2 characters), the message is segmented and charged accordingly. For long body text, consider using the send_as_mms parameter. | true |

#### Example JSON Structure
```json
{
  "label" : "Send SMS",
  "name" : "sendSMS",
  "parameters" : {
    "To" : "",
    "From" : "",
    "Body" : ""
  },
  "type" : "twilio/v1/sendSMS"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| body | STRING |  |
| numSegments | STRING |  |
| direction | STRING |  |
| from | OBJECT <details> <summary> Properties </summary> {STRING\(rawNumber)} </details> |  |
| to | STRING |  |
| dateUpdated | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(dateTime), STRING\(zoneId)} </details> |  |
| price | STRING |  |
| errorMessage | STRING |  |
| uri | STRING |  |
| accountSid | STRING |  |
| numMedia | STRING |  |
| status | STRING |  |
| messagingServiceSid | STRING |  |
| sid | STRING |  |
| dateSent | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(dateTime), STRING\(zoneId)} </details> |  |
| dateCreated | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(dateTime), STRING\(zoneId)} </details> |  |
| errorCode | INTEGER |  |
| currency | OBJECT <details> <summary> Properties </summary> {STRING\(currencyCode), INTEGER\(defaultFractionDigits), INTEGER\(numericCode)} </details> |  |
| apiVersion | STRING |  |
| subresourceUris | OBJECT <details> <summary> Properties </summary> {} </details> |  |




#### Output Example
```json
{
  "body" : "",
  "numSegments" : "",
  "direction" : "",
  "from" : {
    "rawNumber" : ""
  },
  "to" : "",
  "dateUpdated" : {
    "dateTime" : "2021-01-01T00:00:00",
    "zoneId" : ""
  },
  "price" : "",
  "errorMessage" : "",
  "uri" : "",
  "accountSid" : "",
  "numMedia" : "",
  "status" : "",
  "messagingServiceSid" : "",
  "sid" : "",
  "dateSent" : {
    "dateTime" : "2021-01-01T00:00:00",
    "zoneId" : ""
  },
  "dateCreated" : {
    "dateTime" : "2021-01-01T00:00:00",
    "zoneId" : ""
  },
  "errorCode" : 1,
  "currency" : {
    "currencyCode" : "",
    "defaultFractionDigits" : 1,
    "numericCode" : 1
  },
  "apiVersion" : "",
  "subresourceUris" : { }
}
```




