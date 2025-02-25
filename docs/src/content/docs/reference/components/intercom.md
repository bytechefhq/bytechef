---
title: "Intercom"
description: "Intercom is the complete AI-first customer service solution, giving exceptional experiences for support teams with AI agent, AI copilot, tickets, ..."
---

Intercom is the complete AI-first customer service solution, giving exceptional experiences for support teams with AI agent, AI copilot, tickets, ...


Categories: customer-support


Type: intercom/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client id | STRING |  | true |
| clientSecret | Client secret | STRING |  | true |





<hr />



## Actions


### Create Contact
Name: createContact

Create new contact

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| role | Contact Role | STRING <details> <summary> Options </summary> user, lead </details> | Role of the contact | true |
| email | Contact Email | STRING | Email of the contact | true |
| name | Contact Name | STRING | Name of the contact | false |
| phone | Contact Phone | STRING | Phone of the contact must start with a "+" sign | false |
| avatar | Contact Image | STRING | Image of the contact | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| type | STRING |
| id | STRING |
| role | STRING |
| email | STRING |
| phone | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "role" : "",
    "email" : "",
    "name" : "",
    "phone" : "",
    "avatar" : ""
  },
  "type" : "intercom/v1/createContact"
}
```


### Get Contact
Name: getContact

Get a single Contact

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Contact ID | STRING |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| type | STRING |
| id | STRING |
| role | STRING |
| email | STRING |
| phone | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "Get Contact",
  "name" : "getContact",
  "parameters" : {
    "id" : ""
  },
  "type" : "intercom/v1/getContact"
}
```


### Send Message
Name: sendMessage

Send a new message

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| message_type | Message Type | STRING <details> <summary> Options </summary> inapp, email </details> | In app message or email message | true |
| subject | Title | STRING | Title of the Email/Message | true |
| body | Content | STRING | Content of the message | true |
| template | Template | STRING <details> <summary> Options </summary> plain, personal </details> | The style of the outgoing message | true |
| to | To | STRING | ID of the contact to send the message to. | true |


#### Output



Type: OBJECT





#### JSON Example
```json
{
  "label" : "Send Message",
  "name" : "sendMessage",
  "parameters" : {
    "message_type" : "",
    "subject" : "",
    "body" : "",
    "template" : "",
    "to" : ""
  },
  "type" : "intercom/v1/sendMessage"
}
```




