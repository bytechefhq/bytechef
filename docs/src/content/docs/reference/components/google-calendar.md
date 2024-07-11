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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />



## Triggers


### New or Updated Event
Triggers when an event is added or updated

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Calendar identifier | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
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
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |






<hr />



## Actions


### Create event
Creates an event

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Calendar identifier | STRING | SELECT  |
| Title | STRING | TEXT  |
| All day event? | BOOLEAN | SELECT  |
| Start date | DATE | DATE  |
| End date | DATE | DATE  |
| Start date time | DATE_TIME | DATE_TIME  |
| End date time | DATE_TIME | DATE_TIME  |
| Description | STRING | TEXT  |
| Location | STRING | TEXT  |
| Attachments | ARRAY | ARRAY_BUILDER  |
| Attendees | ARRAY | ARRAY_BUILDER  |
| Guest can invite others | BOOLEAN | SELECT  |
| Guest can modify | BOOLEAN | SELECT  |
| Guest can see other guests | BOOLEAN | SELECT  |
| Send updates | STRING | SELECT  |
| Use default reminders | BOOLEAN | SELECT  |
| Reminders | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
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
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Create Quick Event
Add Quick Calendar Event

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Calendar identifier | STRING | SELECT  |
| Text | STRING | TEXT  |
| Send updates | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
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
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Find events
Find events in your calendar

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Calendar identifier | STRING | SELECT  |
| Event type | ARRAY | ARRAY_BUILDER  |
| Max results | INTEGER | INTEGER  |
| Search terms | STRING | TEXT  |
| Time max | DATE_TIME | DATE_TIME  |
| Time min | DATE_TIME | DATE_TIME  |


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
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |





