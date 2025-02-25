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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Add Labels
Name: addLabels

Add labels to an email in your Gmail account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Message ID | STRING | ID of the message to add labels | true |
| labelIds | Labels | ARRAY <details> <summary> Items </summary> [STRING] </details> | Labels to add to this message. You can add up to 100 labels with each update. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| labelIds | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| threadId | STRING |




#### JSON Example
```json
{
  "label" : "Add Labels",
  "name" : "addLabels",
  "parameters" : {
    "id" : "",
    "labelIds" : [ "" ]
  },
  "type" : "googleMail/v1/addLabels"
}
```


### Delete Mail
Name: deleteMail

Delete an email from your Gmail account permanently via Id

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Message ID | STRING | The ID of the message to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Mail",
  "name" : "deleteMail",
  "parameters" : {
    "id" : ""
  },
  "type" : "googleMail/v1/deleteMail"
}
```


### Get Mail
Name: getMail

Get an email from your Gmail account via Id

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Message ID | STRING | The ID of the message to retrieve. | true |
| format | Format | STRING <details> <summary> Options </summary> simple, minimal, full, raw, metadata </details> | The format to return the message in. | false |
| metadataHeaders | Metadata headers | ARRAY <details> <summary> Items </summary> [STRING] </details> | When given and format is METADATA, only include headers specified. | false |


#### JSON Example
```json
{
  "label" : "Get Mail",
  "name" : "getMail",
  "parameters" : {
    "id" : "",
    "format" : "",
    "metadataHeaders" : [ "" ]
  },
  "type" : "googleMail/v1/getMail"
}
```


### Get Thread
Name: getThread

Gets the specified thread.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Thread ID | STRING | The ID of the thread to retrieve. | true |
| format | Format | STRING <details> <summary> Options </summary> simple, minimal, full, raw, metadata </details> | The format to return the message in. | false |
| metadataHeaders | Metadata headers | ARRAY <details> <summary> Items </summary> [STRING] </details> | When given and format is METADATA, only include headers specified. | false |


#### JSON Example
```json
{
  "label" : "Get Thread",
  "name" : "getThread",
  "parameters" : {
    "id" : "",
    "format" : "",
    "metadataHeaders" : [ "" ]
  },
  "type" : "googleMail/v1/getThread"
}
```


### Reply to Email
Name: replyToEmail

Send a reply to an email message.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Message ID | STRING | The ID of the message to reply to. | true |
| to | To | ARRAY <details> <summary> Items </summary> [STRING] </details> | Recipients email addresses. | true |
| bcc | Bcc | ARRAY <details> <summary> Items </summary> [STRING] </details> | Bcc recipients email addresses. | false |
| cc | Cc | ARRAY <details> <summary> Items </summary> [STRING] </details> | Cc recipients email addresses. | false |
| body | Body | STRING | Body text of the email | true |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | A list of attachments to send with the email. | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| labelIds | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| threadId | STRING |




#### JSON Example
```json
{
  "label" : "Reply to Email",
  "name" : "replyToEmail",
  "parameters" : {
    "id" : "",
    "to" : [ "" ],
    "bcc" : [ "" ],
    "cc" : [ "" ],
    "body" : "",
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ]
  },
  "type" : "googleMail/v1/replyToEmail"
}
```


### Search Email
Name: searchEmail

Lists the messages in the user's mailbox.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| maxResults | Max Results | NUMBER | Maximum number of messages to return. | false |
| pageToken | Page Token | STRING | Page token to retrieve a specific page of results in the list. | false |
| from | From | STRING | The address sending the mail | false |
| to | To | STRING | The address receiving the new mail | false |
| subject | Subject | STRING | Words in the subject line | false |
| category | Category | STRING <details> <summary> Options </summary> primary, social, promotions, updates, forums, reservations, purchases </details> | Messages in a certain category | false |
| labelIds | Labels | ARRAY <details> <summary> Items </summary> [STRING] </details> | Only return messages with labels that match all of the specified label IDs. Messages in a thread might have labels that other messages in the same thread don't have. | false |
| includeSpamTrash | Include Spam Trash | BOOLEAN <details> <summary> Options </summary> true, false </details> | Include messages from SPAM and TRASH in the results. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| messages | ARRAY <details> <summary> Items </summary> [{STRING\(id), STRING\(threadId)}] </details> |
| nextPageToken | STRING |
| resultSizeEstimate | NUMBER |




#### JSON Example
```json
{
  "label" : "Search Email",
  "name" : "searchEmail",
  "parameters" : {
    "maxResults" : 0.0,
    "pageToken" : "",
    "from" : "",
    "to" : "",
    "subject" : "",
    "category" : "",
    "labelIds" : [ "" ],
    "includeSpamTrash" : false
  },
  "type" : "googleMail/v1/searchEmail"
}
```


### Send Email
Name: sendEmail

Sends the specified message to the recipients in the To, Cc, and Bcc headers.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| to | To | ARRAY <details> <summary> Items </summary> [STRING] </details> | Recipients email addresses. | true |
| subject | Subject | STRING | Subject of the email. | true |
| bcc | Bcc | ARRAY <details> <summary> Items </summary> [STRING] </details> | Bcc recipients email addresses. | false |
| cc | Cc | ARRAY <details> <summary> Items </summary> [STRING] </details> | Cc recipients email addresses. | false |
| replyTo | Reply To | ARRAY <details> <summary> Items </summary> [STRING] </details> | Reply-to email addresses. | false |
| body | Body | STRING | Body text of the email | true |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | A list of attachments to send with the email. | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| labelIds | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| threadId | STRING |




#### JSON Example
```json
{
  "label" : "Send Email",
  "name" : "sendEmail",
  "parameters" : {
    "to" : [ "" ],
    "subject" : "",
    "bcc" : [ "" ],
    "cc" : [ "" ],
    "replyTo" : [ "" ],
    "body" : "",
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ]
  },
  "type" : "googleMail/v1/sendEmail"
}
```




## Triggers


### New Email
Name: newEmail

Triggers when new mail is found in your Gmail inbox.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| topicName | Topic Name | STRING | Must be 3-255 characters, start with a letter, and contain only the following characters: letters, numbers, dashes (-), periods (.), underscores (_), tildes (~), percents (%) or plus signs (+). Cannot start with goog. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(threadId), [STRING]\(labelIds), STRING\(snippet), STRING\(historyId), NUMBER\(internalDate), {STRING\(partId), STRING\(mimeType), STRING\(filename), [{STRING\(name), STRING\(value)}]\(headers), {STRING\(attachmentId), INTEGER\(size), STRING\(data)}\(body), []\(parts)}\(payload), INTEGER\(sizeEstimate), STRING\(raw)} </details> |




#### JSON Example
```json
{
  "label" : "New Email",
  "name" : "newEmail",
  "parameters" : {
    "topicName" : ""
  },
  "type" : "googleMail/v1/newEmail"
}
```


### New Email Polling
Name: newEmailPolling

Periodically checks your Gmail inbox for any new incoming emails.

Type: POLLING


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(threadId)} </details> |




#### JSON Example
```json
{
  "label" : "New Email Polling",
  "name" : "newEmailPolling",
  "type" : "googleMail/v1/newEmailPolling"
}
```


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
