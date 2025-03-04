---
title: "Microsoft Outlook 365"
description: "Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, calendar, contacts, and tasks to streamline communication and organization."
---

Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, calendar, contacts, and tasks to streamline communication and organization.


Categories: communication, calendars-and-scheduling


Type: microsoftOutlook365/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |
| tenantId | Tenant Id | STRING |  | true |





<hr />



## Actions


### Create Event
Name: createEvent

Creates an event in the specified calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendar | Calendar ID | STRING |  | true |
| subject | Subject | STRING | The subject of the event. | false |
| allDay | All Day Event? | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | true |
| start | Start Date | DATE | The start date of the event. | true |
| end | End Date | DATE | The end date of the event. | true |
| start | Start Date Time | DATE_TIME | The start time of the event. | true |
| end | End Date Time | DATE_TIME | The end time of the event. | true |
| attendees | Attendees | ARRAY <details> <summary> Items </summary> [STRING] </details> | The attendees of the event. | false |
| isOnlineMeeting | Is Online Meeting? | BOOLEAN <details> <summary> Options </summary> true, false </details> | Is the event an online meeting? | false |
| reminderMinutesBeforeStart | Reminder Minutes Before Start | INTEGER | The number of minutes before the event start time that the reminder alert occurs. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| iCalUId | STRING | ID for an event across calendars, |
| id | STRING | ID of the event. |
| subject | STRING | The text of the event's subject line. |
| startTime | DATE_TIME | Start time of the event. |
| endTime | DATE_TIME | End time of the event. |
| attendees | ARRAY <details> <summary> Items </summary> [STRING] </details> | The attendees for the event. |
| isOnlineMeeting | BOOLEAN <details> <summary> Options </summary> true, false </details> | Indicates whether the event is an online meeting. |
| onlineMeetingUrl | STRING | URL for an online meeting. |
| reminderMinutesBeforeStart | BOOLEAN <details> <summary> Options </summary> true, false </details> | The number of minutes before the event start time that the reminder alert occurs. |




#### JSON Example
```json
{
  "label" : "Create Event",
  "name" : "createEvent",
  "parameters" : {
    "calendar" : "",
    "subject" : "",
    "allDay" : false,
    "start" : "2021-01-01T00:00:00",
    "end" : "2021-01-01T00:00:00",
    "attendees" : [ "" ],
    "isOnlineMeeting" : false,
    "reminderMinutesBeforeStart" : 1
  },
  "type" : "microsoftOutlook365/v1/createEvent"
}
```


### Delete Event
Name: deleteEvent

Deletes an event from the specified calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendar | Calendar ID | STRING |  | true |
| event | Event ID | STRING <details> <summary> Depends On </summary> calendar </details> | Id of the event to delete. | true |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Delete Event",
  "name" : "deleteEvent",
  "parameters" : {
    "calendar" : "",
    "event" : ""
  },
  "type" : "microsoftOutlook365/v1/deleteEvent"
}
```


### Get Events
Name: getEvents

Gets a list of events in specified calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendar | Calendar ID | STRING |  | true |
| dateRange | Date Range | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(from), DATE_TIME\(to)} </details> | Date range to find events that exist in this range. | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(iCalUId), STRING\(id), STRING\(subject), DATE_TIME\(startTime), DATE_TIME\(endTime), [STRING]\(attendees), BOOLEAN\(isOnlineMeeting), STRING\(onlineMeetingUrl), BOOLEAN\(reminderMinutesBeforeStart)} </details> |  |




#### JSON Example
```json
{
  "label" : "Get Events",
  "name" : "getEvents",
  "parameters" : {
    "calendar" : "",
    "dateRange" : {
      "from" : "2021-01-01T00:00:00",
      "to" : "2021-01-01T00:00:00"
    }
  },
  "type" : "microsoftOutlook365/v1/getEvents"
}
```


### Get Free Time Slots
Name: getFreeTimeSlots

Get free time slots from the Microsoft Outlook 365 calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendar | Calendar ID | STRING |  | true |
| dateRange | Date Range | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(from), DATE_TIME\(to)} </details> | Date range to find free time. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(startTime), DATE_TIME\(endTime)} </details> |  |




#### JSON Example
```json
{
  "label" : "Get Free Time Slots",
  "name" : "getFreeTimeSlots",
  "parameters" : {
    "calendar" : "",
    "dateRange" : {
      "from" : "2021-01-01T00:00:00",
      "to" : "2021-01-01T00:00:00"
    }
  },
  "type" : "microsoftOutlook365/v1/getFreeTimeSlots"
}
```


### Get Mail
Name: getMail

Get a specific message

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Message Id | STRING | Id of the message. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | ID of the message. |
| subject | STRING | Subject of the message. |
| bodyPreview | STRING | The first 255 characters of the message body. |
| body | OBJECT <details> <summary> Properties </summary> {STRING\(contentType), STRING\(content)} </details> | The body of the message. It can be in HTML or text format. |
| from | OBJECT <details> <summary> Properties </summary> {{STRING\(name), STRING\(address)}\(emailAddress)} </details> | The owner of the mailbox from which the message is sent. |




#### JSON Example
```json
{
  "label" : "Get Mail",
  "name" : "getMail",
  "parameters" : {
    "id" : ""
  },
  "type" : "microsoftOutlook365/v1/getMail"
}
```


### Reply to Email
Name: replyToEmail

Creates a new reply to email.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Message ID | STRING | Id of the message to reply to. | true |
| comment | Comment | STRING | Content of the reply to the email. | true |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Reply to Email",
  "name" : "replyToEmail",
  "parameters" : {
    "id" : "",
    "comment" : ""
  },
  "type" : "microsoftOutlook365/v1/replyToEmail"
}
```


### Search Email
Name: searchEmail

Get the messages in the signed-in user's mailbox

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| from | From | STRING | The address sending the mail | false |
| to | To | STRING | The address receiving the new mail | false |
| subject | Subject | STRING | Words in the subject line | false |
| category | Category | STRING | Messages in a certain category | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(subject), STRING\(bodyPreview), {STRING\(contentType), STRING\(content)}\(body), {{STRING\(name), STRING\(address)}\(emailAddress)}\(from)} </details> |  |




#### JSON Example
```json
{
  "label" : "Search Email",
  "name" : "searchEmail",
  "parameters" : {
    "from" : "",
    "to" : "",
    "subject" : "",
    "category" : ""
  },
  "type" : "microsoftOutlook365/v1/searchEmail"
}
```


### Send Email
Name: sendEmail

Send the message.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| from | From | OBJECT <details> <summary> Properties </summary> {{{STRING\(address), STRING\(name)}\(emailAddress)}\(recipient)} </details> | The owner of the mailbox from which the message is sent. In most cases, this value is the same as the sender property, except for sharing or delegation scenarios. The value must correspond to the actual mailbox used. | null |
| toRecipients | To Recipients | ARRAY <details> <summary> Items </summary> [{{STRING\(address), STRING\(name)}\(emailAddress)}] </details> | The To: recipients for the message. | true |
| subject | Subject | STRING | The subject of the message. | true |
| bccRecipients | Bcc Recipients | ARRAY <details> <summary> Items </summary> [{{STRING\(address), STRING\(name)}\(emailAddress)}] </details> | The Bcc recipients for the message. | false |
| ccRecipients | Cc Recipients | ARRAY <details> <summary> Items </summary> [{{STRING\(address), STRING\(name)}\(emailAddress)}] </details> | The Cc recipients for the message. | false |
| replyTo | Reply To | ARRAY <details> <summary> Items </summary> [{{STRING\(address), STRING\(name)}\(emailAddress)}] </details> | The email addresses to use when replying. | false |
| body | Body | OBJECT <details> <summary> Properties </summary> {STRING\(contentType), STRING\(content), STRING\(content)} </details> | The body of the message. It can be in HTML or text format. | true |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Send Email",
  "name" : "sendEmail",
  "parameters" : {
    "from" : {
      "recipient" : {
        "emailAddress" : {
          "address" : "",
          "name" : ""
        }
      }
    },
    "toRecipients" : [ {
      "emailAddress" : {
        "address" : "",
        "name" : ""
      }
    } ],
    "subject" : "",
    "bccRecipients" : [ {
      "emailAddress" : {
        "address" : "",
        "name" : ""
      }
    } ],
    "ccRecipients" : [ {
      "emailAddress" : {
        "address" : "",
        "name" : ""
      }
    } ],
    "replyTo" : [ {
      "emailAddress" : {
        "address" : "",
        "name" : ""
      }
    } ],
    "body" : {
      "contentType" : "",
      "content" : ""
    }
  },
  "type" : "microsoftOutlook365/v1/sendEmail"
}
```




## Triggers


### New Email
Name: newEmail

Triggers when new mail is received.

Type: POLLING


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(subject), STRING\(bodyPreview), {STRING\(contentType), STRING\(content)}\(body), {{STRING\(name), STRING\(address)}\(emailAddress)}\(from)} </details> |  |




#### JSON Example
```json
{
  "label" : "New Email",
  "name" : "newEmail",
  "type" : "microsoftOutlook365/v1/newEmail"
}
```


<hr />

