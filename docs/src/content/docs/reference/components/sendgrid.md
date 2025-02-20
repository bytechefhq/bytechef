---
title: "Sendgrid"
description: "Trusted for reliable email delivery at scale."
---

Trusted for reliable email delivery at scale.


Categories: communication, marketing-automation


Type: sendgrid/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | Token | STRING | TEXT |  | true |





<hr />



## Actions


### Send Email
Name: sendEmail

Sends an email.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| from | From | STRING | TEXT | Email address from which you want to send. | true |
| to | To | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Email addresses which you want to send to. | true |
| cc | CC | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Email address which receives a copy. | false |
| subject | Subject | STRING | TEXT | Subject of your email | true |
| text | Message Body | STRING | RICH_TEXT | This is the message you want to send | true |
| type | Message Type | STRING <details> <summary> Options </summary> text/plain, text/html </details> | SELECT | Message type for your content | true |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | ARRAY_BUILDER | A list of attachments you want to include with the email. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| type | STRING | TEXT |
| from | STRING | TEXT |
| to | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| subject | STRING | TEXT |
| text | STRING | TEXT |
| attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Send Email",
  "name" : "sendEmail",
  "parameters" : {
    "from" : "",
    "to" : [ "" ],
    "cc" : [ "" ],
    "subject" : "",
    "text" : "",
    "type" : "",
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ]
  },
  "type" : "sendgrid/v1/sendEmail"
}
```




