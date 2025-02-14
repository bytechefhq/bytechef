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
Name: addAttendeesToEvent

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
Name: createEvent

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
Name: createQuickEvent

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
Name: deleteEvent

Deletes an event from Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| calendarId | Calendar Identifier | STRING | SELECT  |  | true  |
| eventId | Event ID | STRING | SELECT  |  ID of the event to delete.  |  true  |




### Get Events
Name: getEvents

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
Name: getFreeTimeSlots

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
Name: updateEvent

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

## Connection Setup

### Create OAuth 2.0 Application

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click on the project dropdown in the top navigation bar.
3. Click **New Project**.
4. Enter a project name and click **Create**.
5. Click on the project dropdown again.
6. Select the project you just created.
7. Go to the **APIs & Services**.
8. Go to the **OAuth consent screen**.
9. Click **Get Started**.
10. Enter an App name and add user support email. Click **Next**.
11. Select your Audience and click **Next**.
12. Add email addresses and click **Next**.
13. Agree to the terms and click **Create**.
14. Go to **Data Access**.
15. Click on **Add or Remove Scopes**.
16. Select all necessary scopes.
17. Click **Update**.
18. Click **Save**.
19. Go to the **Clients**.
20. Click on **Create Client**.
21. Click on application type dropdown.
22. Choose **Web application** as the application type.
23. Click on **Add Uri**.
24. Enter a redirect URI, e.g., `https://app.bytechef.io/callback`, `http://127.0.0.1:5173/callback`. Click **Create**.
25. Click on the client you just created.
26. Copy the **Client ID** and **Client Secret**. Use these credentials to create a connection in ByteChef.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

### Enable Google Calendar API

1. In the [Google Cloud Console](https://console.cloud.google.com/), select your project.
2. Go to the **APIs & Services**.
3. Click on **ENABLE APIS AND SERVICES**.
4. Search for "calendar" in the search bar.
5. Click on **Google Calendar API**.
6. Click **Enable**.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)">
<iframe src="https://www.guidejar.com/embed/c1fe8158-a72f-45ed-942e-c1bab5802afa?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>
