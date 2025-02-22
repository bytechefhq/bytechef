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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Add Attendees to Event
Name: addAttendeesToEvent

Invites one or more person to an existing event.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| eventId | Event ID | STRING <details> <summary> Depends On </summary> calendarId </details> | ID of the event to add attendees to. | true |
| attendees | Attendees | ARRAY <details> <summary> Items </summary> [STRING] </details> | The attendees of the event. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| iCalUID | STRING |
| id | STRING |
| summary | STRING |
| startTime | DATE_TIME |
| endTime | DATE_TIME |
| etag | STRING |
| eventType | STRING |
| htmlLink | STRING |
| status | STRING |
| location | STRING |
| hangoutLink | STRING |
| attendees | ARRAY <details> <summary> Items </summary> [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] </details> |
| attachments | ARRAY <details> <summary> Items </summary> [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] </details> |
| reminders | OBJECT <details> <summary> Properties </summary> {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} </details> |




#### JSON Example
```json
{
  "label" : "Add Attendees to Event",
  "name" : "addAttendeesToEvent",
  "parameters" : {
    "calendarId" : "",
    "eventId" : "",
    "attendees" : [ "" ]
  },
  "type" : "googleCalendar/v1/addAttendeesToEvent"
}
```


### Create Event
Name: createEvent

Creates an event

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| summary | Title | STRING | Title of the event. | false |
| allDay | All Day Event? | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | true |
| start | Start Date | DATE | The start date of the event. | true |
| end | End Date | DATE | The end date of the event. | true |
| start | Start Date Time | DATE_TIME | The (inclusive) start time of the event. For a recurring event, this is the start time of the first instance. | true |
| end | End Date Time | DATE_TIME | The (exclusive) end time of the event. For a recurring event, this is the end time of the first instance. | true |
| description | Description | STRING | Description of the event. Can contain HTML. | false |
| location | Location | STRING | Geographic location of the event as free-form text. | false |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> |  | false |
| attendees | Attendees | ARRAY <details> <summary> Items </summary> [STRING] </details> | The attendees of the event. | false |
| guestsCanInviteOthers | Guest Can Invite Others | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether attendees other than the organizer can invite others to the event. | false |
| guestsCanModify | Guest Can Modify | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether attendees other than the organizer can modify the event. | false |
| guestsCanSeeOtherGuests | Guest Can See Other Guests | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether attendees other than the organizer can see who the event's attendees are. | false |
| sendUpdates | Send Updates | STRING <details> <summary> Options </summary> all, externalOnly, none </details> | Whether to send notifications about the creation of the new event. Note that some emails might still be sent. | false |
| useDefault | Use Default Reminders | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether the default reminders of the calendar apply to the event. | true |
| reminders | Reminders | ARRAY <details> <summary> Items </summary> [{STRING\(method), INTEGER\(minutes)}] </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| iCalUID | STRING |
| id | STRING |
| summary | STRING |
| startTime | DATE_TIME |
| endTime | DATE_TIME |
| etag | STRING |
| eventType | STRING |
| htmlLink | STRING |
| status | STRING |
| location | STRING |
| hangoutLink | STRING |
| attendees | ARRAY <details> <summary> Items </summary> [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] </details> |
| attachments | ARRAY <details> <summary> Items </summary> [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] </details> |
| reminders | OBJECT <details> <summary> Properties </summary> {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} </details> |




#### JSON Example
```json
{
  "label" : "Create Event",
  "name" : "createEvent",
  "parameters" : {
    "calendarId" : "",
    "summary" : "",
    "allDay" : false,
    "start" : "2021-01-01T00:00:00",
    "end" : "2021-01-01T00:00:00",
    "description" : "",
    "location" : "",
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ],
    "attendees" : [ "" ],
    "guestsCanInviteOthers" : false,
    "guestsCanModify" : false,
    "guestsCanSeeOtherGuests" : false,
    "sendUpdates" : "",
    "useDefault" : false,
    "reminders" : [ {
      "method" : "",
      "minutes" : 1
    } ]
  },
  "type" : "googleCalendar/v1/createEvent"
}
```


### Create Quick Event
Name: createQuickEvent

Add Quick Calendar Event

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| text | Text | STRING | The text describing the event to be created. | true |
| sendUpdates | Send Updates | STRING <details> <summary> Options </summary> all, externalOnly, none </details> | Whether to send notifications about the creation of the new event. Note that some emails might still be sent. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| iCalUID | STRING |
| id | STRING |
| summary | STRING |
| startTime | DATE_TIME |
| endTime | DATE_TIME |
| etag | STRING |
| eventType | STRING |
| htmlLink | STRING |
| status | STRING |
| location | STRING |
| hangoutLink | STRING |
| attendees | ARRAY <details> <summary> Items </summary> [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] </details> |
| attachments | ARRAY <details> <summary> Items </summary> [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] </details> |
| reminders | OBJECT <details> <summary> Properties </summary> {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} </details> |




#### JSON Example
```json
{
  "label" : "Create Quick Event",
  "name" : "createQuickEvent",
  "parameters" : {
    "calendarId" : "",
    "text" : "",
    "sendUpdates" : ""
  },
  "type" : "googleCalendar/v1/createQuickEvent"
}
```


### Delete Event
Name: deleteEvent

Deletes an event from Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| eventId | Event ID | STRING <details> <summary> Depends On </summary> calendarId </details> | ID of the event to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Event",
  "name" : "deleteEvent",
  "parameters" : {
    "calendarId" : "",
    "eventId" : ""
  },
  "type" : "googleCalendar/v1/deleteEvent"
}
```


### Get Events
Name: getEvents

List events from the specified Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| eventType | Event Type | ARRAY <details> <summary> Items </summary> [STRING] </details> | Event types to return. | false |
| maxResults | Max Results | INTEGER | Maximum number of events returned on one result page. The number of events in the resulting page may be less than this value, or none at all, even if there are more events matching the query. Incomplete pages can be detected by a non-empty nextPageToken field in the response. | false |
| q | Search Terms | STRING | Free text search terms to find events that match these terms in the following fields: summary, description, location, attendee's displayName, attendee's email, workingLocationProperties.officeLocation.buildingId, workingLocationProperties.officeLocation.deskId, workingLocationProperties.officeLocation.label and workingLocationProperties.customLocation.label | false |
| dateRange | Date Range | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(from), DATE_TIME\(to)} </details> | Date range to find events that exist in this range. | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(iCalUID), STRING\(id), STRING\(summary), DATE_TIME\(startTime), DATE_TIME\(endTime), STRING\(etag), STRING\(eventType), STRING\(htmlLink), STRING\(status), STRING\(location), STRING\(hangoutLink), [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}]\(attendees), [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}]\(attachments), {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)}\(reminders)} </details> |




#### JSON Example
```json
{
  "label" : "Get Events",
  "name" : "getEvents",
  "parameters" : {
    "calendarId" : "",
    "eventType" : [ "" ],
    "maxResults" : 1,
    "q" : "",
    "dateRange" : {
      "from" : "2021-01-01T00:00:00",
      "to" : "2021-01-01T00:00:00"
    }
  },
  "type" : "googleCalendar/v1/getEvents"
}
```


### Get Free Time Slots
Name: getFreeTimeSlots

Get free time slots from Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| dateRange | Date Range | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(from), DATE_TIME\(to)} </details> | Date range to find free time. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(startTime), DATE_TIME\(endTime)} </details> |




#### JSON Example
```json
{
  "label" : "Get Free Time Slots",
  "name" : "getFreeTimeSlots",
  "parameters" : {
    "calendarId" : "",
    "dateRange" : {
      "from" : "2021-01-01T00:00:00",
      "to" : "2021-01-01T00:00:00"
    }
  },
  "type" : "googleCalendar/v1/getFreeTimeSlots"
}
```


### Update Event
Name: updateEvent

Updates event in Google Calendar.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |
| eventId | Event ID | STRING <details> <summary> Depends On </summary> calendarId </details> | ID of the event to update. | true |
| summary | Title | STRING | New title of the event. | false |
| allDay | All Day Event? | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | false |
| start | Start Date | DATE | New start date of the event. | true |
| end | End Date | DATE | New end date of the event. | true |
| start | Start Date Time | DATE_TIME | New (inclusive) start time of the event. For a recurring event, this is the start time of the first instance. | true |
| end | End Date Time | DATE_TIME | New (exclusive) end time of the event. For a recurring event, this is the end time of the first instance. | true |
| description | Description | STRING | New description of the event. Can contain HTML. | false |
| attendees | Attendees | ARRAY <details> <summary> Items </summary> [STRING] </details> | New attendees of the event. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| iCalUID | STRING |
| id | STRING |
| summary | STRING |
| startTime | DATE_TIME |
| endTime | DATE_TIME |
| etag | STRING |
| eventType | STRING |
| htmlLink | STRING |
| status | STRING |
| location | STRING |
| hangoutLink | STRING |
| attendees | ARRAY <details> <summary> Items </summary> [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] </details> |
| attachments | ARRAY <details> <summary> Items </summary> [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] </details> |
| reminders | OBJECT <details> <summary> Properties </summary> {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} </details> |




#### JSON Example
```json
{
  "label" : "Update Event",
  "name" : "updateEvent",
  "parameters" : {
    "calendarId" : "",
    "eventId" : "",
    "summary" : "",
    "allDay" : false,
    "start" : "2021-01-01T00:00:00",
    "end" : "2021-01-01T00:00:00",
    "description" : "",
    "attendees" : [ "" ]
  },
  "type" : "googleCalendar/v1/updateEvent"
}
```




## Triggers


### New or Updated Event
Name: newOrUpdatedEvent

Triggers when an event is added or updated

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| calendarId | Calendar Identifier | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| iCalUID | STRING |
| id | STRING |
| summary | STRING |
| startTime | DATE_TIME |
| endTime | DATE_TIME |
| etag | STRING |
| eventType | STRING |
| htmlLink | STRING |
| status | STRING |
| location | STRING |
| hangoutLink | STRING |
| attendees | ARRAY <details> <summary> Items </summary> [{INTEGER\(additionalGuests), STRING\(comment), STRING\(displayName), STRING\(email), STRING\(id), BOOLEAN\(optional), BOOLEAN\(organizer), BOOLEAN\(resource), STRING\(responseStatus), BOOLEAN\(self)}] </details> |
| attachments | ARRAY <details> <summary> Items </summary> [{STRING\(fileId), STRING\(fileUrl), STRING\(iconLink), STRING\(mimeType), STRING\(title)}] </details> |
| reminders | OBJECT <details> <summary> Properties </summary> {[{STRING\(method), INTEGER\(minutes)}]\(overrides), BOOLEAN\(useDefault)} </details> |




#### JSON Example
```json
{
  "label" : "New or Updated Event",
  "name" : "newOrUpdatedEvent",
  "parameters" : {
    "calendarId" : ""
  },
  "type" : "googleCalendar/v1/newOrUpdatedEvent"
}
```


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
