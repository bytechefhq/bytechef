---
title: "Gmail"
description: "Gmail is a widely used email service by Google, offering free and feature-rich communication, organization, and storage capabilities accessible through web browsers and mobile apps."
---

Gmail is a widely used email service by Google, offering free and feature-rich communication, organization, and storage capabilities accessible through web browsers and mobile apps.


Categories: communication


Type: googleMail/v1

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


### Add Labels
Add labels to an email in your Gmail account.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Message ID | STRING | SELECT  |  ID of the message to add labels  |  true  |
| labelIds | Labels | [STRING\($label)] | ARRAY_BUILDER  |  Labels to add to this message. You can add up to 100 labels with each update.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| labelIds | [STRING] | ARRAY_BUILDER  |
| threadId | STRING | TEXT  |






### Delete Mail
Delete an email from your Gmail account permanently via Id

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Message ID | STRING | SELECT  |  The ID of the message to delete.  |  true  |




### Get Mail
Get an email from your Gmail account via Id

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Message ID | STRING | SELECT  |  The ID of the message to retrieve.  |  true  |
| format | Format | STRING | SELECT  |  The format to return the message in.  |  false  |
| metadataHeaders | Metadata headers | [STRING] | ARRAY_BUILDER  |  When given and format is METADATA, only include headers specified.  |  false  |




### Get Thread
Gets the specified thread.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Thread ID | STRING | SELECT  |  The ID of the thread to retrieve.  |  true  |
| format | Format | STRING | SELECT  |  The format to return the message in.  |  false  |
| metadataHeaders | Metadata headers | [STRING] | ARRAY_BUILDER  |  When given and format is METADATA, only include headers specified.  |  false  |




### Reply to Email
Send a reply to an email message.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Message ID | STRING | SELECT  |  The ID of the message to reply to.  |  true  |
| to | To | [STRING\($email)] | ARRAY_BUILDER  |  Recipients email addresses.  |  true  |
| bcc | Bcc | [STRING\($email)] | ARRAY_BUILDER  |  Bcc recipients email addresses.  |  false  |
| cc | Cc | [STRING\($email)] | ARRAY_BUILDER  |  Cc recipients email addresses.  |  false  |
| body | Body | STRING | TEXT_AREA  |  Body text of the email  |  true  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| labelIds | [STRING] | ARRAY_BUILDER  |
| threadId | STRING | TEXT  |






### Search Email
Lists the messages in the user's mailbox.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| maxResults | Max Results | NUMBER | NUMBER  |  Maximum number of messages to return.  |  false  |
| pageToken | Page Token | STRING | TEXT  |  Page token to retrieve a specific page of results in the list.  |  false  |
| from | From | STRING | TEXT  |  The address sending the mail  |  false  |
| to | To | STRING | TEXT  |  The address receiving the new mail  |  false  |
| subject | Subject | STRING | TEXT  |  Words in the subject line  |  false  |
| category | Category | STRING | SELECT  |  Messages in a certain category  |  false  |
| labelIds | Labels | [STRING] | ARRAY_BUILDER  |  Only return messages with labels that match all of the specified label IDs. Messages in a thread might have labels that other messages in the same thread don't have.  |  false  |
| includeSpamTrash | Include Spam Trash | BOOLEAN | SELECT  |  Include messages from SPAM and TRASH in the results.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| messages | [{STRING\(id), STRING\(threadId)}] | ARRAY_BUILDER  |
| nextPageToken | STRING | TEXT  |
| resultSizeEstimate | NUMBER | NUMBER  |






### Send Email
Sends the specified message to the recipients in the To, Cc, and Bcc headers.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| to | To | [STRING\($email)] | ARRAY_BUILDER  |  Recipients email addresses.  |  true  |
| subject | Subject | STRING | TEXT  |  Subject of the email.  |  true  |
| bcc | Bcc | [STRING\($email)] | ARRAY_BUILDER  |  Bcc recipients email addresses.  |  false  |
| cc | Cc | [STRING\($email)] | ARRAY_BUILDER  |  Cc recipients email addresses.  |  false  |
| replyTo | Reply To | [STRING\($email)] | ARRAY_BUILDER  |  Reply-to email addresses.  |  false  |
| body | Body | STRING | RICH_TEXT  |  Body text of the email  |  true  |
| attachments | Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| labelIds | [STRING] | ARRAY_BUILDER  |
| threadId | STRING | TEXT  |








## Triggers


### New Email
Triggers when new mail is found in your Gmail inbox.

Type: STATIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| topicName | Topic Name | STRING | TEXT  |  Must be 3-255 characters, start with a letter, and contain only the following characters:
letters, numbers, dashes (-), periods (.), underscores (_), tildes (~), percents (%) or
plus signs (+). Cannot start with goog.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(threadId), [STRING]\(labelIds), STRING\(snippet), STRING\(historyId), NUMBER\(internalDate), {STRING\(partId), STRING\(mimeType), STRING\(filename), [{STRING\(name), STRING\(value)}]\(headers), {STRING\(attachmentId), INTEGER\(size), STRING\(data)}\(body), []\(parts)}\(payload), INTEGER\(sizeEstimate), STRING\(raw)} | OBJECT_BUILDER  |







### New Email Polling
Periodically checks your Gmail inbox for any new incoming emails.

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
|  | {STRING\(id), STRING\(threadId)} | OBJECT_BUILDER  |







<hr />

<hr />

# Additional instructions
<hr />

![anl-c-google-mail-md](https://static.scarf.sh/a.png?x-pxid=2bfa99dc-2ceb-4a8f-9b0e-26650c2f0f95)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Gmail API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/2d7279c7-91c3-43c9-9004-99f08d7e30ff?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
