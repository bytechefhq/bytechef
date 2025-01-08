---
title: "Microsoft Outlook 365"
description: "Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, calendar, contacts, and tasks to streamline communication and organization."
---
## Reference
<hr />

Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, calendar, contacts, and tasks to streamline communication and organization.


Categories: [communication, calendars-and-scheduling]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Tenant Id | STRING | TEXT  |  |





<hr />



## Triggers


### New Email
Triggers when new mail is received.

#### Type: POLLING
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(subject), STRING\(bodyPreview), {STRING\(contentType), STRING\(content)}\(body), {{STRING\(name), STRING\(address)}\(emailAddress)}\(from)} | OBJECT_BUILDER  |







<hr />



## Actions


### Create Event
Creates an event in the specified calendar.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar | STRING | SELECT  |  |
| Subject | STRING | TEXT  |  The subject of the event.  |
| All Day Event? | BOOLEAN | SELECT  |  |
| Start Date | DATE | DATE  |  The start date of the event.  |
| End Date | DATE | DATE  |  The end date of the event.  |
| Start Date Time | DATE_TIME | DATE_TIME  |  The start time of the event.  |
| End Date Time | DATE_TIME | DATE_TIME  |  The end time of the event.  |
| Attendees | [STRING\($emailAddress)] | ARRAY_BUILDER  |  The attendees of the event.  |
| Is Online Meeting? | BOOLEAN | SELECT  |  Is the event an online meeting?  |
| Reminder Minutes Before Start | INTEGER | INTEGER  |  The number of minutes before the event start time that the reminder alert occurs.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| [STRING] | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |






### Delete Event
Deletes an event from the specified calendar.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar | STRING | SELECT  |  |
| Event | STRING | SELECT  |  Event to delete.  |




### Get Events
Gets a list of events in specified calendar.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar | STRING | SELECT  |  |
| Date Range | {DATE_TIME\(from), DATE_TIME\(to)} | OBJECT_BUILDER  |  Date range to find events that exist in this range.  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(iCalUId), STRING\(id), STRING\(subject), DATE_TIME\(startTime), DATE_TIME\(endTime), [STRING]\(attendees), BOOLEAN\(isOnlineMeeting), STRING\(onlineMeetingUrl), BOOLEAN\(reminderMinutesBeforeStart)} | OBJECT_BUILDER  |






### Get Free Time Slots
Get free time slots from the Microsoft Outlook 365 calendar.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar | STRING | SELECT  |  |
| Date Range | {DATE_TIME\(from), DATE_TIME\(to)} | OBJECT_BUILDER  |  Date range to find free time.  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {DATE_TIME\(startTime), DATE_TIME\(endTime)} | OBJECT_BUILDER  |






### Get Mail
Get a specific message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message Id | STRING | SELECT  |  Id of the message  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(contentType), STRING\(content)} | OBJECT_BUILDER  |
| {{STRING\(name), STRING\(address)}\(emailAddress)} | OBJECT_BUILDER  |






### Reply to Email
Creates a new reply to email.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message ID | STRING | SELECT  |  Id of the message to reply to.  |
| Comment | STRING | TEXT  |  Content of the reply to the email.  |




### Search Email
Get the messages in the signed-in user's mailbox

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | TEXT  |  The address sending the mail  |
| To | STRING | TEXT  |  The address receiving the new mail  |
| Subject | STRING | TEXT  |  Words in the subject line  |
| Category | STRING | SELECT  |  Messages in a certain category  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(subject), STRING\(bodyPreview), {STRING\(contentType), STRING\(content)}\(body), {{STRING\(name), STRING\(address)}\(emailAddress)}\(from)} | OBJECT_BUILDER  |






### Send Email
Send the message.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | {{{STRING\(address), STRING\(name)}\(emailAddress)}\(recipient)} | OBJECT_BUILDER  |  The owner of the mailbox from which the message is sent. In most cases, this value is the same as the sender property, except for sharing or delegation scenarios. The value must correspond to the actual mailbox used.  |
| To Recipients | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The To: recipients for the message.  |
| Subject | STRING | TEXT  |  The subject of the message.  |
| Bcc Recipients | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The Bcc recipients for the message.  |
| Cc Recipients | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The Cc recipients for the message.  |
| Reply To | [{{STRING\(address), STRING\(name)}\(emailAddress)}\($recipient)] | ARRAY_BUILDER  |  The email addresses to use when replying.  |
| Body | {INTEGER\(contentType), STRING\(content), STRING\(content)} | OBJECT_BUILDER  |  The body of the message. It can be in HTML or text format.  |




