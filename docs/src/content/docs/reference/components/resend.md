---
title: "Resend"
description: "Resend is the email API for developers."
---

Resend is the email API for developers.


Categories: marketing-automation


Type: resend/v1

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

Send an email

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| from | From | STRING | EMAIL | Sender email address. | true |
| to | To | ARRAY <details> <summary> Items </summary> [STRING\($email)] </details> | ARRAY_BUILDER | Recipients email addresses. | true |
| subject | Subject | STRING | TEXT | Email subject. | true |
| bcc | Bcc | ARRAY <details> <summary> Items </summary> [STRING\($email)] </details> | ARRAY_BUILDER | Bcc recipients email addresses. | false |
| cc | Cc | ARRAY <details> <summary> Items </summary> [STRING\($email)] </details> | ARRAY_BUILDER | Cc recipients email addresses. | false |
| reply_to | Reply To | ARRAY <details> <summary> Items </summary> [STRING\($email)] </details> | ARRAY_BUILDER | Reply-to email addresses. | false |
| contentType | Content Type | STRING <details> <summary> Options </summary> HTML, TEXT </details> | SELECT |  | true |
| html | HTML | STRING | RICH_TEXT | The HTML version of the message. | false |
| text | Text | STRING | TEXT_AREA | The plain text version of the message. | false |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Custom headers to add to the email. | false |
| attachments | Attachments | ARRAY <details> <summary> Items </summary> [FILE_ENTRY] </details> | ARRAY_BUILDER | A list of attachments to send with the email. | false |
| tags | | ARRAY <details> <summary> Items </summary> [{STRING\(name), STRING\(value)}] </details> | ARRAY_BUILDER |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Send Email",
  "name" : "sendEmail",
  "parameters" : {
    "from" : "",
    "to" : [ "" ],
    "subject" : "",
    "bcc" : [ "" ],
    "cc" : [ "" ],
    "reply_to" : [ "" ],
    "contentType" : "",
    "html" : "",
    "text" : "",
    "headers" : { },
    "attachments" : [ {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    } ],
    "tags" : [ {
      "name" : "",
      "value" : ""
    } ]
  },
  "type" : "resend/v1/sendEmail"
}
```




<hr />

# Additional instructions
<hr />

## CONNECTION

[API key location](https://resend.com/api-keys)
