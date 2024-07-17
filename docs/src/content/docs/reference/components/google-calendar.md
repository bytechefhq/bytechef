---
title: "Google Calendar"
description: "Google Calendar is a web-based application that allows users to schedule and organize events, appointments, and reminders, synchronizing across multiple devices."
---
## Reference
<hr />

Google Calendar is a web-based application that allows users to schedule and organize events, appointments, and reminders, synchronizing across multiple devices.


Categories: [CALENDARS_AND_SCHEDULING]


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





<hr />



## Triggers


### New or Updated Event
Triggers when an event is added or updated

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar identifier | STRING | SELECT  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| [{STRING(fileId), STRING(fileUrl), STRING(iconLink), STRING(mimeType), STRING(title)}] | ARRAY_BUILDER  |
| [{INTEGER(additionalGuests), STRING(comment), STRING(displayName), STRING(email), STRING(id), BOOLEAN(optional), BOOLEAN(organizer), BOOLEAN(resource), STRING(responseStatus), BOOLEAN(self)}] | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| {STRING(conferenceId), {STRING(iconUri), {STRING(type)}(key), STRING(name)}(conferenceSolution), {{STRING(type)}(conferenceSolutionKey), STRING(requestId), {STRING(statusCode)}(status)}(createRequest), [{STRING(accessCode), [STRING](entryPointFeatures), STRING(entryPointType), STRING(label), STRING(meetingCode), STRING(passcode), STRING(password), STRING(pin), STRING(regionCode), STRING(uri)}](entryPoints), STRING(notes), {{{}(parameters)}(addOnParameters)}(parameters), STRING(signature)} | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {{}(private), {}(shared)} | OBJECT_BUILDER  |
| {STRING(autoDeclineMode), STRING(chatStatus), STRING(declineMessage)} | OBJECT_BUILDER  |
| {STRING(display), INTEGER(height), STRING(iconLink), STRING(link), {}(preferences), STRING(title), STRING(type), STRING(width)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)} | OBJECT_BUILDER  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| {STRING(autoDeclineMode), STRING(declineMessage)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| {[{STRING(method), INTEGER(minutes)}](overrides), BOOLEAN(useDefault)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| {STRING(title), STRING(url)} | OBJECT_BUILDER  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| {{STRING(label)}(customLocation), {}(homeOffice), {STRING(buildingId), STRING(deskId), STRING(floorId), STRING(floorSectionId), STRING(label)}(officeLocation), STRING(type)} | OBJECT_BUILDER  |






<hr />



## Actions


### Create event
Creates an event

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar identifier | STRING | SELECT  |  |
| Title | STRING | TEXT  |  Title of the event.  |
| All day event? | BOOLEAN | SELECT  |  |
| Start date | DATE | DATE  |  The start date of the event.  |
| End date | DATE | DATE  |  The end date of the event.  |
| Start date time | DATE_TIME | DATE_TIME  |  The (inclusive) start time of the event. For a recurring event, this is the start time of the first instance.  |
| End date time | DATE_TIME | DATE_TIME  |  The (exclusive) end time of the event. For a recurring event, this is the end time of the first instance.  |
| Description | STRING | TEXT  |  Description of the event. Can contain HTML.  |
| Location | STRING | TEXT  |  Geographic location of the event as free-form text.  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  |
| Attendees | [STRING($email)] | ARRAY_BUILDER  |  The attendees of the event.  |
| Guest can invite others | BOOLEAN | SELECT  |  Whether attendees other than the organizer can invite others to the event.  |
| Guest can modify | BOOLEAN | SELECT  |  Whether attendees other than the organizer can modify the event.  |
| Guest can see other guests | BOOLEAN | SELECT  |  Whether attendees other than the organizer can see who the event's attendees are.  |
| Send updates | STRING | SELECT  |  Whether to send notifications about the creation of the new event. Note that some emails might still be sent.  |
| Use default reminders | BOOLEAN | SELECT  |  Whether the default reminders of the calendar apply to the event.  |
| Reminders | [{STRING(method), INTEGER(minutes)}] | ARRAY_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| [{STRING(fileId), STRING(fileUrl), STRING(iconLink), STRING(mimeType), STRING(title)}] | ARRAY_BUILDER  |
| [{INTEGER(additionalGuests), STRING(comment), STRING(displayName), STRING(email), STRING(id), BOOLEAN(optional), BOOLEAN(organizer), BOOLEAN(resource), STRING(responseStatus), BOOLEAN(self)}] | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| {STRING(conferenceId), {STRING(iconUri), {STRING(type)}(key), STRING(name)}(conferenceSolution), {{STRING(type)}(conferenceSolutionKey), STRING(requestId), {STRING(statusCode)}(status)}(createRequest), [{STRING(accessCode), [STRING](entryPointFeatures), STRING(entryPointType), STRING(label), STRING(meetingCode), STRING(passcode), STRING(password), STRING(pin), STRING(regionCode), STRING(uri)}](entryPoints), STRING(notes), {{{}(parameters)}(addOnParameters)}(parameters), STRING(signature)} | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {{}(private), {}(shared)} | OBJECT_BUILDER  |
| {STRING(autoDeclineMode), STRING(chatStatus), STRING(declineMessage)} | OBJECT_BUILDER  |
| {STRING(display), INTEGER(height), STRING(iconLink), STRING(link), {}(preferences), STRING(title), STRING(type), STRING(width)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)} | OBJECT_BUILDER  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| {STRING(autoDeclineMode), STRING(declineMessage)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| {[{STRING(method), INTEGER(minutes)}](overrides), BOOLEAN(useDefault)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| {STRING(title), STRING(url)} | OBJECT_BUILDER  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| {{STRING(label)}(customLocation), {}(homeOffice), {STRING(buildingId), STRING(deskId), STRING(floorId), STRING(floorSectionId), STRING(label)}(officeLocation), STRING(type)} | OBJECT_BUILDER  |





### Create Quick Event
Add Quick Calendar Event

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar identifier | STRING | SELECT  |  |
| Text | STRING | TEXT  |  The text describing the event to be created.  |
| Send updates | STRING | SELECT  |  Whether to send notifications about the creation of the new event. Note that some emails might still be sent.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| [{STRING(fileId), STRING(fileUrl), STRING(iconLink), STRING(mimeType), STRING(title)}] | ARRAY_BUILDER  |
| [{INTEGER(additionalGuests), STRING(comment), STRING(displayName), STRING(email), STRING(id), BOOLEAN(optional), BOOLEAN(organizer), BOOLEAN(resource), STRING(responseStatus), BOOLEAN(self)}] | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| {STRING(conferenceId), {STRING(iconUri), {STRING(type)}(key), STRING(name)}(conferenceSolution), {{STRING(type)}(conferenceSolutionKey), STRING(requestId), {STRING(statusCode)}(status)}(createRequest), [{STRING(accessCode), [STRING](entryPointFeatures), STRING(entryPointType), STRING(label), STRING(meetingCode), STRING(passcode), STRING(password), STRING(pin), STRING(regionCode), STRING(uri)}](entryPoints), STRING(notes), {{{}(parameters)}(addOnParameters)}(parameters), STRING(signature)} | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {{}(private), {}(shared)} | OBJECT_BUILDER  |
| {STRING(autoDeclineMode), STRING(chatStatus), STRING(declineMessage)} | OBJECT_BUILDER  |
| {STRING(display), INTEGER(height), STRING(iconLink), STRING(link), {}(preferences), STRING(title), STRING(type), STRING(width)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)} | OBJECT_BUILDER  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| {STRING(autoDeclineMode), STRING(declineMessage)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| {[{STRING(method), INTEGER(minutes)}](overrides), BOOLEAN(useDefault)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| {STRING(title), STRING(url)} | OBJECT_BUILDER  |
| {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| {{STRING(label)}(customLocation), {}(homeOffice), {STRING(buildingId), STRING(deskId), STRING(floorId), STRING(floorSectionId), STRING(label)}(officeLocation), STRING(type)} | OBJECT_BUILDER  |





### Find events
Find events in your calendar

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Calendar identifier | STRING | SELECT  |  |
| Event type | [STRING] | ARRAY_BUILDER  |  Event types to return.  |
| Max results | INTEGER | INTEGER  |  Maximum number of events returned on one result page. The number of events in the resulting page may be less than this value, or none at all, even if there are more events matching the query. Incomplete pages can be detected by a non-empty nextPageToken field in the response.  |
| Search terms | STRING | TEXT  |  Free text search terms to find events that match these terms in the following fields: summary, description, location, attendee's displayName, attendee's email, workingLocationProperties.officeLocation.buildingId, workingLocationProperties.officeLocation.deskId, workingLocationProperties.officeLocation.label and workingLocationProperties.customLocation.label  |
| Time max | DATE_TIME | DATE_TIME  |  Upper bound (exclusive) for an event's start time to filter by. The default is not to filter by start time. Must be an RFC3339 timestamp with mandatory time zone offset, for example, 2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but are ignored.  |
| Time min | DATE_TIME | DATE_TIME  |  Lower bound (exclusive) for an event's end time to filter by. The default is not to filter by end time. Must be an RFC3339 timestamp with mandatory time zone offset, for example, 2011-06-03T10:00:00-07:00, 2011-06-03T10:00:00Z. Milliseconds may be provided but are ignored.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(method), INTEGER(minutes)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{BOOLEAN(anyoneCanAddSelf), [{STRING(fileId), STRING(fileUrl), STRING(iconLink), STRING(mimeType), STRING(title)}](attachments), [{INTEGER(additionalGuests), STRING(comment), STRING(displayName), STRING(email), STRING(id), BOOLEAN(optional), BOOLEAN(organizer), BOOLEAN(resource), STRING(responseStatus), BOOLEAN(self)}](attendees), BOOLEAN(attendeesOmitted), STRING(colorId), {STRING(conferenceId), {STRING(iconUri), {STRING(type)}(key), STRING(name)}(conferenceSolution), {{STRING(type)}(conferenceSolutionKey), STRING(requestId), {STRING(statusCode)}(status)}(createRequest), [{STRING(accessCode), [STRING](entryPointFeatures), STRING(entryPointType), STRING(label), STRING(meetingCode), STRING(passcode), STRING(password), STRING(pin), STRING(regionCode), STRING(uri)}](entryPoints), STRING(notes), {{{}(parameters)}(addOnParameters)}(parameters), STRING(signature)}(conferenceData), DATE_TIME(created), {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)}(creator), STRING(description), {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)}(end), BOOLEAN(endTimeUnspecified), STRING(etag), STRING(eventType), {{}(private), {}(shared)}(extendedProperties), {STRING(autoDeclineMode), STRING(chatStatus), STRING(declineMessage)}(focusTimeProperties), {STRING(display), INTEGER(height), STRING(iconLink), STRING(link), {}(preferences), STRING(title), STRING(type), STRING(width)}(gadget), BOOLEAN(guestsCanInviteOthers), BOOLEAN(guestsCanModify), BOOLEAN(guestsCanSeeOtherGuests), STRING(hangoutLink), STRING(htmlLink), STRING(iCalUID), STRING(id), STRING(kind), STRING(location), BOOLEAN(locked), {STRING(displayName), STRING(email), STRING(id), BOOLEAN(self)}(organizer), {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)}(originalStartTime), {STRING(autoDeclineMode), STRING(declineMessage)}(outOfOfficeProperties), BOOLEAN(privateCopy), [STRING](recurrence), STRING(recurringEventId), {[{STRING(method), INTEGER(minutes)}](overrides), BOOLEAN(useDefault)}(reminders), INTEGER(sequence), {STRING(title), STRING(url)}(source), {DATE_TIME(date), DATE_TIME(dateTime), STRING(timeZone)}(start), STRING(status), STRING(summary), STRING(transparency), DATE_TIME(updated), STRING(visibility), {{STRING(label)}(customLocation), {}(homeOffice), {STRING(buildingId), STRING(deskId), STRING(floorId), STRING(floorSectionId), STRING(label)}(officeLocation), STRING(type)}(workingLocationProperties)}] | ARRAY_BUILDER  |





<hr />

# Additional instructions
<hr />

![anl-c-google-calendar-md](https://static.scarf.sh/a.png?x-pxid=88f94c85-204a-4086-bfae-12024a15535d)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

[Guidejar](https://guidejar.com/guides/fec74020-26bb-43dd-814c-f8b907f6f45b) tutorial.

[Turning on Calendar API](https://guidejar.com/guides/c1fe8158-a72f-45ed-942e-c1bab5802afa)
