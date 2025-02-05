---
title: "Google Calendar"
description: "Google Calendar is a web-based application that allows users to schedule and organize events, appointments, and reminders, synchronizing across multiple devices."
---

Google Calendar is a web-based application that allows users to schedule and organize events, appointments, and reminders, synchronizing across multiple devices.


Categories: calendars-and-scheduling


Type: googleCalendar/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Add Attendees to Event
Invites one or more person to an existing event.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| eventId | Event ID | STRING | SELECT  |  ID of the event to add attendees to.  |  true  |
| attendees | Attendees | [STRING\($email)] | ARRAY_BUILDER  |  The attendees of the event.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| iCalUID | STRING | TEXT  |
| id | STRING | TEXT  |
| summary | STRING | TEXT  |
| startTime | DATE_TIME | DATE_TIME  |
| endTime | DATE_TIME | DATE_TIME  |
| etag | STRING | TEXT  |
| eventType | STRING | TEXT  |
| htmlLink | STRING | TEXT  |
| status | STRING | TEXT  |
| location | STRING | TEXT  |
| hangoutLink | STRING | TEXT  |
| attendees | [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] | ARRAY_BUILDER  |
| attachments | [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] | ARRAY_BUILDER  |
| reminders | {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} | OBJECT_BUILDER  |






### Create Event
Creates an event

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| summary | Title | STRING | TEXT  |  Title of the event.  |  false  |
| allDay | All Day Event? | BOOLEAN | SELECT  |  | true  |
| start | Start Date | DATE | DATE  |  The start date of the event.  |  true  |
| end | End Date | DATE | DATE  |  The end date of the event.  |  true  |
| start | Start Date Time | DATE_TIME | DATE_TIME  |  The (inclusive) start time of the event. For a recurring event, this is the start time of the first instance.  |  true  |
| end | End Date Time | DATE_TIME | DATE_TIME  |  The (exclusive) end time of the event. For a recurring event, this is the end time of the first instance.  |  true  |
| description | Description | STRING | TEXT  |  Description of the event. Can contain HTML.  |  false  |
| location | Location | STRING | TEXT  |  Geographic location of the event as free-form text.  |  false  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  | false  |
| attendees | Attendees | [STRING\($email)] | ARRAY_BUILDER  |  The attendees of the event.  |  false  |
| guestsCanInviteOthers | Guest Can Invite Others | BOOLEAN | SELECT  |  Whether attendees other than the organizer can invite others to the event.  |  false  |
| guestsCanModify | Guest Can Modify | BOOLEAN | SELECT  |  Whether attendees other than the organizer can modify the event.  |  false  |
| guestsCanSeeOtherGuests | Guest Can See Other Guests | BOOLEAN | SELECT  |  Whether attendees other than the organizer can see who the event's attendees are.  |  false  |
| sendUpdates | Send Updates | STRING | SELECT  |  Whether to send notifications about the creation of the new event. Note that some emails might still be sent.  |  false  |
| useDefault | Use Default Reminders | BOOLEAN | SELECT  |  Whether the default reminders of the calendar apply to the event.  |  true  |
| reminders | Reminders | [{STRING\(method), INTEGER\(minutes)}] | ARRAY_BUILDER  |  | false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| iCalUID | STRING | TEXT  |
| id | STRING | TEXT  |
| summary | STRING | TEXT  |
| startTime | DATE_TIME | DATE_TIME  |
| endTime | DATE_TIME | DATE_TIME  |
| etag | STRING | TEXT  |
| eventType | STRING | TEXT  |
| htmlLink | STRING | TEXT  |
| status | STRING | TEXT  |
| location | STRING | TEXT  |
| hangoutLink | STRING | TEXT  |
| attendees | [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] | ARRAY_BUILDER  |
| attachments | [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] | ARRAY_BUILDER  |
| reminders | {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} | OBJECT_BUILDER  |






### Create Quick Event
Add Quick Calendar Event

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| text | Text | STRING | TEXT  |  The text describing the event to be created.  |  true  |
| sendUpdates | Send Updates | STRING | SELECT  |  Whether to send notifications about the creation of the new event. Note that some emails might still be sent.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| iCalUID | STRING | TEXT  |
| id | STRING | TEXT  |
| summary | STRING | TEXT  |
| startTime | DATE_TIME | DATE_TIME  |
| endTime | DATE_TIME | DATE_TIME  |
| etag | STRING | TEXT  |
| eventType | STRING | TEXT  |
| htmlLink | STRING | TEXT  |
| status | STRING | TEXT  |
| location | STRING | TEXT  |
| hangoutLink | STRING | TEXT  |
| attendees | [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] | ARRAY_BUILDER  |
| attachments | [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] | ARRAY_BUILDER  |
| reminders | {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} | OBJECT_BUILDER  |






### Delete Event
Deletes an event from Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| eventId | Event ID | STRING | SELECT  |  ID of the event to delete.  |  true  |




### Get Events
List events from the specified Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| eventType | Event Type | [STRING] | ARRAY_BUILDER  |  Event types to return.  |  false  |
| maxResults | Max Results | INTEGER | INTEGER  |  Maximum number of events returned on one result page. The number of events in the resulting page may be less than this value, or none at all, even if there are more events matching the query. Incomplete pages can be detected by a non-empty nextPageToken field in the response.  |  false  |
| q | Search Terms | STRING | TEXT  |  Free text search terms to find events that match these terms in the following fields: summary, description, location, attendee's displayName, attendee's email, workingLocationProperties.officeLocation.buildingId, workingLocationProperties.officeLocation.deskId, workingLocationProperties.officeLocation.label and workingLocationProperties.customLocation.label  |  false  |
| dateRange | Date Range | {DATE_TIME\(from), DATE_TIME\(to)} | OBJECT_BUILDER  |  Date range to find events that exist in this range.  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(iCalUID), STRING\(id), STRING\(summary), DATE_TIME\(startTime), DATE_TIME\(endTime), STRING\(etag), STRING\(eventType), STRING\(htmlLink), STRING\(status), STRING\(location), STRING\(hangoutLink), [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}]\(attendees), [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}]\(attachments), {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)}\(reminders)} | OBJECT_BUILDER  |






### Get Free Time Slots
Get free time slots from Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| dateRange | Date Range | {DATE_TIME\(from), DATE_TIME\(to)} | OBJECT_BUILDER  |  Date range to find free time.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {DATE_TIME\(startTime), DATE_TIME\(endTime)} | OBJECT_BUILDER  |






### Update Event
Updates event in Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| eventId | Event ID | STRING | SELECT  |  ID of the event to update.  |  true  |
| summary | Title | STRING | TEXT  |  New title of the event.  |  false  |
| allDay | All Day Event? | BOOLEAN | SELECT  |  | false  |
| start | Start Date | DATE | DATE  |  New start date of the event.  |  true  |
| end | End Date | DATE | DATE  |  New end date of the event.  |  true  |
| start | Start Date Time | DATE_TIME | DATE_TIME  |  New (inclusive) start time of the event. For a recurring event, this is the start time of the first instance.  |  true  |
| end | End Date Time | DATE_TIME | DATE_TIME  |  New (exclusive) end time of the event. For a recurring event, this is the end time of the first instance.  |  true  |
| description | Description | STRING | TEXT  |  New description of the event. Can contain HTML.  |  false  |
| attendees | Attendees | [STRING\($email)] | ARRAY_BUILDER  |  New attendees of the event.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| iCalUID | STRING | TEXT  |
| id | STRING | TEXT  |
| summary | STRING | TEXT  |
| startTime | DATE_TIME | DATE_TIME  |
| endTime | DATE_TIME | DATE_TIME  |
| etag | STRING | TEXT  |
| eventType | STRING | TEXT  |
| htmlLink | STRING | TEXT  |
| status | STRING | TEXT  |
| location | STRING | TEXT  |
| hangoutLink | STRING | TEXT  |
| attendees | [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] | ARRAY_BUILDER  |
| attachments | [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] | ARRAY_BUILDER  |
| reminders | {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} | OBJECT_BUILDER  |








## Triggers


### New or Updated Event
Triggers when an event is added or updated

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| iCalUID | STRING | TEXT  |
| id | STRING | TEXT  |
| summary | STRING | TEXT  |
| startTime | DATE_TIME | DATE_TIME  |
| endTime | DATE_TIME | DATE_TIME  |
| etag | STRING | TEXT  |
| eventType | STRING | TEXT  |
| htmlLink | STRING | TEXT  |
| status | STRING | TEXT  |
| location | STRING | TEXT  |
| hangoutLink | STRING | TEXT  |
| attendees | [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] | ARRAY_BUILDER  |
| attachments | [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] | ARRAY_BUILDER  |
| reminders | {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} | OBJECT_BUILDER  |







<hr />

<hr />

# Additional instructions
<hr />

![anl-c-google-calendar-md](https://static.scarf.sh/a.png?x-pxid=88f94c85-204a-4086-bfae-12024a15535d)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Calendar API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/c1fe8158-a72f-45ed-942e-c1bab5802afa?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
