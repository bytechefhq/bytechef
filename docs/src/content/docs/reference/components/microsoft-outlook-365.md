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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Tenant Id | STRING | TEXT  |  |





<hr />





## Actions


### Get Mail
Get a specific message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message id | STRING | SELECT  |  Id of the message  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





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



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Send Email
Send the message.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | {{{STRING(address), STRING(name)}(emailAddress)}(recipient)} | OBJECT_BUILDER  |  The owner of the mailbox from which the message is sent. In most cases, this value is the same as the sender property, except for sharing or delegation scenarios. The value must correspond to the actual mailbox used.  |
| To recipients | [{{STRING(address), STRING(name)}(emailAddress)}($recipient)] | ARRAY_BUILDER  |  The To: recipients for the message.  |
| Subject | STRING | TEXT  |  The subject of the message.  |
| Bcc recipients | [{{STRING(address), STRING(name)}(emailAddress)}($recipient)] | ARRAY_BUILDER  |  The Bcc recipients for the message.  |
| Cc recipients | [{{STRING(address), STRING(name)}(emailAddress)}($recipient)] | ARRAY_BUILDER  |  The Cc recipients for the message.  |
| Reply to | [{{STRING(address), STRING(name)}(emailAddress)}($recipient)] | ARRAY_BUILDER  |  The email addresses to use when replying.  |
| Body | {STRING(content), STRING(contentType)} | OBJECT_BUILDER  |  The body of the message. It can be in HTML or text format.  |




