---
title: "Email"
description: "The Email connector sends emails using an SMTP email server."
---

The Email connector sends emails using an SMTP email server.


Categories: communication, helpers


Type: email/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING |  | true |
| password | Password | STRING |  | true |





<hr />



## Actions


### Send
Name: send

Send an email to any address.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| from | From Email | STRING | From who to send the email. | true |
| to | To Email | ARRAY <details> <summary> Items </summary> [STRING] </details> | Who to send the email to. | true |
| cc | CC Email | ARRAY <details> <summary> Items </summary> [STRING] </details> | Who to CC on the email. | null |
| bcc | BCC Email | ARRAY <details> <summary> Items </summary> [STRING] </details> | Who to BCC on the email. | null |
| replyTo | Reply To | ARRAY <details> <summary> Items </summary> [STRING] </details> | When someone replies to this email, where should it go to? | null |
| subject | Subject | STRING | Your email subject. | true |
| content | Content | STRING | Your email content. Will be sent as a HTML email. | null |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | A list of attachments to send with the email. | null |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Send",
  "name" : "send",
  "parameters" : {
    "from" : "",
    "to" : [ "" ],
    "cc" : [ "" ],
    "bcc" : [ "" ],
    "replyTo" : [ "" ],
    "subject" : "",
    "content" : "",
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ]
  },
  "type" : "email/v1/send"
}
```




