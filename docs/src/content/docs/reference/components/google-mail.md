---
title: "Google Mail"
description: "Google Mail, commonly known as Gmail, is a widely used email service by Google, offering free and feature-rich communication, organization, and storage capabilities accessible through web browsers and mobile apps."
---
## Reference
<hr />

Google Mail, commonly known as Gmail, is a widely used email service by Google, offering free and feature-rich communication, organization, and storage capabilities accessible through web browsers and mobile apps.


Categories: [COMMUNICATION]


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


### New Email
Triggers when new mail is found in your Gmail inbox.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Topic name | STRING | TEXT  |  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null






<hr />



## Actions


### Get Mail
Get an email from your Gmail account via Id

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message ID | STRING | SELECT  |  The ID of the message to retrieve.  |
| Format | STRING | SELECT  |  The format to return the message in.  |
| Metadata headers | [STRING] | ARRAY_BUILDER  |  When given and format is METADATA, only include headers specified.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| {STRING(partId), STRING(mimeType), STRING(filename), [{STRING(name), STRING(value)}](headers), {STRING(attachmentId), INTEGER(size), STRING(data)}(body), [](parts)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |





### Get Thread
Gets the specified thread.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Thread ID | STRING | SELECT  |  The ID of the thread to retrieve.  |
| Format | STRING | SELECT  |  The format to return the message in.  |
| Metadata headers | [STRING] | ARRAY_BUILDER  |  When given and format is METADATA, only include headers specified.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(id), STRING(threadId), [STRING](labelIds), STRING(snippet), STRING(historyId), NUMBER(internalDate), {STRING(partId), STRING(mimeType), STRING(filename), [{STRING(name), STRING(value)}](headers), {STRING(attachmentId), INTEGER(size), STRING(data)}(body), [](parts)}(payload), INTEGER(sizeEstimate), STRING(raw)}] | ARRAY_BUILDER  |





### Search Email
Lists the messages in the user's mailbox.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Max results | NUMBER | NUMBER  |  Maximum number of messages to return.  |
| Page token | STRING | TEXT  |  Page token to retrieve a specific page of results in the list.  |
| From | STRING | TEXT  |  The address sending the mail  |
| To | STRING | TEXT  |  The address receiving the new mail  |
| Subject | STRING | TEXT  |  Words in the subject line  |
| Category | STRING | SELECT  |  Messages in a certain category  |
| Label | STRING | SELECT  |    |
| Label IDs | [STRING] | ARRAY_BUILDER  |  Only return messages with labels that match all of the specified label IDs. Messages in a thread might have labels that other messages in the same thread don't have.  |
| Include spam trash | BOOLEAN | SELECT  |  Include messages from SPAM and TRASH in the results.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{STRING(id), STRING(threadId), [STRING](labelIds), STRING(snippet), STRING(historyId), NUMBER(internalDate), {STRING(partId), STRING(mimeType), STRING(filename), [{STRING(name), STRING(value)}](headers), {STRING(attachmentId), INTEGER(size), STRING(data)}(body), [](parts)}(payload), INTEGER(sizeEstimate), STRING(raw)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| NUMBER | NUMBER  |





### Send Email
Sends the specified message to the recipients in the To, Cc, and Bcc headers.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| From | STRING | TEXT  |  Email address of the sender, the mailbox account.  |
| To | [STRING($email)] | ARRAY_BUILDER  |  Recipients email addresses.  |
| Subject | STRING | TEXT  |  Subject of the email.  |
| Bcc | [STRING($email)] | ARRAY_BUILDER  |  Bcc recipients email addresses.  |
| Cc | [STRING($email)] | ARRAY_BUILDER  |  Cc recipients email addresses.  |
| Reply to | [STRING($email)] | ARRAY_BUILDER  |  Reply-to email addresses.  |
| Body | STRING | TEXT  |  Body text of the email  |
| Attachments | [FILE_ENTRY] | ARRAY_BUILDER  |  A list of attachments to send with the email.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| [STRING] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| {STRING(partId), STRING(mimeType), STRING(filename), [{STRING(name), STRING(value)}](headers), {STRING(attachmentId), INTEGER(size), STRING(data)}(body), [](parts)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |





<hr />

# Additional instructions
<hr />

![anl-c-google-mail-md](https://static.scarf.sh/a.png?x-pxid=2bfa99dc-2ceb-4a8f-9b0e-26650c2f0f95)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

[Guidejar](https://guidejar.com/guides/fec74020-26bb-43dd-814c-f8b907f6f45b) tutorial.

[Turning on GMail API](https://guidejar.com/guides/2d7279c7-91c3-43c9-9004-99f08d7e30ff)
