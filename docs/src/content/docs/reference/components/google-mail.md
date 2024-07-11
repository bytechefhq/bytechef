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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />



## Triggers


### New Email
Triggers when new mail is found in your Gmail inbox.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Topic name | STRING | TEXT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Message ID | STRING | SELECT  |
| Format | STRING | SELECT  |
| Metadata headers | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| OBJECT | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |





### Get Thread
Gets the specified thread.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Thread ID | STRING | SELECT  |
| Format | STRING | SELECT  |
| Metadata headers | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |





### Search Email
Lists the messages in the user's mailbox.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Max results | NUMBER | NUMBER  |
| Page token | STRING | TEXT  |
| From | STRING | TEXT  |
| To | STRING | TEXT  |
| Subject | STRING | TEXT  |
| Category | STRING | SELECT  |
| Label | STRING | SELECT  |
| Label IDs | ARRAY | ARRAY_BUILDER  |
| Include spam trash | BOOLEAN | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| NUMBER | NUMBER  |





### Send Email
Sends the specified message to the recipients in the To, Cc, and Bcc headers.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From | STRING | TEXT  |
| To | ARRAY | ARRAY_BUILDER  |
| Subject | STRING | TEXT  |
| Bcc | ARRAY | ARRAY_BUILDER  |
| Cc | ARRAY | ARRAY_BUILDER  |
| Reply to | ARRAY | ARRAY_BUILDER  |
| Body | STRING | TEXT  |
| Attachments | ARRAY | ARRAY_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| OBJECT | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |





