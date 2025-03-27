---
title: "Brevo"
description: "Brevo is an email marketing platform that offers a cloud-based marketing communication software suite with transactional email, marketing automation, customer-relationship management  and more."
---

Brevo is an email marketing platform that offers a cloud-based marketing communication software suite with transactional email, marketing automation, customer-relationship management  and more.


Categories: marketing-automation


Type: brevo/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING |  | true |
| value | API key | STRING |  | true |





<hr />



## Actions


### Create contact
Name: createContact

Create new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| email | Email | STRING | Email address of the user. | true |
| FIRSTNAME | First name | STRING | First name of the user. | true |
| LASTNAME | Last name | STRING | Last name of the user. | true |

#### Example JSON Structure
```json
{
  "label" : "Create contact",
  "name" : "createContact",
  "parameters" : {
    "email" : "",
    "FIRSTNAME" : "",
    "LASTNAME" : ""
  },
  "type" : "brevo/v1/createContact"
}
```

#### Output



Type: INTEGER








### Update contact
Name: updateContact

Update contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| email | Email | STRING | Email address of the user. | true |
| FIRSTNAME | First name | STRING | First name of the user. | true |
| LASTNAME | Last name | STRING | Last name of the user. | true |

#### Example JSON Structure
```json
{
  "label" : "Update contact",
  "name" : "updateContact",
  "parameters" : {
    "email" : "",
    "FIRSTNAME" : "",
    "LASTNAME" : ""
  },
  "type" : "brevo/v1/updateContact"
}
```

#### Output

This action does not produce any output.




### Send transactional email
Name: sendTransactionalEmail

Send a transactional email.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| senderEmail | Sender email | STRING | Email of the sender from which the emails will be sent. | true |
| senderName | Sender name | STRING | Name of the sender from which the emails will be sent. | true |
| recipientEmail | Recipient email | STRING | Email address of the recipient. | true |
| recipientName | Recipient name | STRING | Name of the recipient. | true |
| subject | Subject | STRING | Subject of the message. | false |
| textContent | Text body | STRING | Plain text body of the message. | false |

#### Example JSON Structure
```json
{
  "label" : "Send transactional email",
  "name" : "sendTransactionalEmail",
  "parameters" : {
    "senderEmail" : "",
    "senderName" : "",
    "recipientEmail" : "",
    "recipientName" : "",
    "subject" : "",
    "textContent" : ""
  },
  "type" : "brevo/v1/sendTransactionalEmail"
}
```

#### Output



Type: STRING










