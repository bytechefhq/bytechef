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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| tenantId | Tenant Id | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Event
Creates an event in the specified calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendar | Calendar ID | STRING | SELECT  |  | true  |
| subject | Subject | STRING | TEXT  |  The subject of the event.  |  false  |
| allDay | All Day Event? | BOOLEAN | SELECT  |  | true  |
| start | Start Date | DATE | DATE  |  The start date of the event.  |  true  |
| end | End Date | DATE | DATE  |  The end date of the event.  |  true  |
| start | Start Date Time | DATE_TIME | DATE_TIME  |  The start time of the event.  |  true  |
| end | End Date Time | DATE_TIME | DATE_TIME  |  The end time of the event.  |  true  |
| attendees | Attendees | [STRING\($emailAddress)] | ARRAY_BUILDER  |  The attendees of the event.  |  false  |
| isOnlineMeeting | Is Online Meeting? | BOOLEAN | SELECT  |  Is the event an online meeting?  |  false  |
| reminderMinutesBeforeStart | Reminder Minutes Before Start | INTEGER | INTEGER  |  The number of minutes before the event start time that the reminder alert occurs.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| iCalUId | STRING | TEXT  |
| id | STRING | TEXT  |
| subject | STRING | TEXT  |
| startTime | DATE_TIME | DATE_TIME  |
| endTime | DATE_TIME | DATE_TIME  |
| attendees | [STRING] | ARRAY_BUILDER  |
| isOnlineMeeting | BOOLEAN | SELECT  |
| onlineMeetingUrl | STRING | TEXT  |
| reminderMinutesBeforeStart | BOOLEAN | SELECT  |






### Delete Event
Deletes an event from the specified calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendar | Calendar ID | STRING | SELECT  |  | true  |
| event | Event ID | STRING | SELECT  |  Id of the event to delete.  |  true  |




### Get Events
Gets a list of events in specified calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendar | Calendar ID | STRING | SELECT  |  | true  |
| dateRange | Date Range | {DATE_TIME\(from), DATE_TIME\(to)} | OBJECT_BUILDER  |  Date range to find events that exist in this range.  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(iCalUId), STRING\(id), STRING\(subject), DATE_TIME\(startTime), DATE_TIME\(endTime), [STRING]\(attendees), BOOLEAN\(isOnlineMeeting), STRING\(onlineMeetingUrl), BOOLEAN\(reminderMinutesBeforeStart)} | OBJECT_BUILDER  |






### Get Free Time Slots
Get free time slots from the Microsoft Outlook 365 calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendar | Calendar ID | STRING | SELECT  |  | true  |
| dateRange | Date Range | {DATE_TIME\(from), DATE_TIME\(to)} | OBJECT_BUILDER  |  Date range to find free time.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {DATE_TIME\(startTime), DATE_TIME\(endTime)} | OBJECT_BUILDER  |






### Get Mail
Get a specific message

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Message Id | STRING | SELECT  |  Id of the message.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| subject | STRING | TEXT  |
| bodyPreview | STRING | TEXT  |
| body | {STRING\(contentType), STRING\(content)} | OBJECT_BUILDER  |
| from | {{STRING\(name), STRING\(address)}\(emailAddress)} | OBJECT_BUILDER  |






### Reply to Email
Creates a new reply to email.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Message ID | STRING | SELECT  |  Id of the message to reply to.  |  true  |
| comment | Comment | STRING | TEXT  |  Content of the reply to the email.  |  true  |




### Search Email
Get the messages in the signed-in user's mailbox

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from | From | STRING | TEXT  |  The address sending the mail  |  false  |
| to | To | STRING | TEXT  |  The address receiving the new mail  |  false  |
| subject | Subject | STRING | TEXT  |  Words in the subject line  |  false  |
| category | Category | STRING | SELECT  |  Messages in a certain category  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(subject), STRING\(bodyPreview), {STRING\(contentType), STRING\(content)}\(body), {{STRING\(name), STRING\(address)}\(emailAddress)}\(from)} | OBJECT_BUILDER  |






### Send Email
Send the message.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from | From | {{{STRING\(address), STRING\(name)}\(emailAddress)}\(recipient)} | OBJECT_BUILDER  |  The owner of the mailbox from which the message is sent. In most cases, this value is the same as the sender property, except for sharing or delegation scenarios. The value must correspond to the actual mailbox used.  |  null  |
| toRecipients | To Recipients | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The To: recipients for the message.  |  true  |
| subject | Subject | STRING | TEXT  |  The subject of the message.  |  true  |
| bccRecipients | Bcc Recipients | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The Bcc recipients for the message.  |  false  |
| ccRecipients | Cc Recipients | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The Cc recipients for the message.  |  false  |
| replyTo | Reply To | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The email addresses to use when replying.  |  false  |
| body | Body | {STRING\(contentType), STRING\(content), STRING\(content)} | OBJECT_BUILDER  |  The body of the message. It can be in HTML or text format.  |  true  |






## Triggers


### New Email
Triggers when new mail is received.

Type: POLLING
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(subject), STRING\(bodyPreview), {STRING\(contentType), STRING\(content)}\(body), {{STRING\(name), STRING\(address)}\(emailAddress)}\(from)} | OBJECT_BUILDER  |







<hr />

