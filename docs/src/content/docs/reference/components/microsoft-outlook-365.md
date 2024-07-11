---
title: "Microsoft Outlook 365"
description: "Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, calendar, contacts, and tasks to streamline communication and organization."
---
## Reference
<hr />

Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, calendar, contacts, and tasks to streamline communication and organization.

Categories: [COMMUNICATION, CALENDARS_AND_SCHEDULING]

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
| Tenant Id | STRING | TEXT  |





<hr />





## Actions


### Get Mail
Get a specific message

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Message id | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Search Email
Get the messages in the signed-in user's mailbox

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From | STRING | TEXT  |
| To | STRING | TEXT  |
| Subject | STRING | TEXT  |
| Category | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Send Email
Send the message.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From | OBJECT | OBJECT_BUILDER  |
| To recipients | ARRAY | ARRAY_BUILDER  |
| Subject | STRING | TEXT  |
| Bcc recipients | ARRAY | ARRAY_BUILDER  |
| Cc recipients | ARRAY | ARRAY_BUILDER  |
| Reply to | ARRAY | ARRAY_BUILDER  |
| Body | OBJECT | OBJECT_BUILDER  |




