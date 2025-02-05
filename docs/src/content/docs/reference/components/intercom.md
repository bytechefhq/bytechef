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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client id | STRING | TEXT  |  | true  |
| clientSecret | Client secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Contact
Create new contact

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| role | Contact Role | STRING | SELECT  |  Role of the contact  |  true  |
| email | Contact Email | STRING | TEXT  |  Email of the contact  |  true  |
| name | Contact Name | STRING | TEXT  |  Name of the contact  |  false  |
| phone | Contact Phone | STRING | TEXT  |  Phone of the contact must start with a "+" sign  |  false  |
| avatar | Contact Image | STRING | TEXT  |  Image of the contact  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| type | STRING | TEXT  |
| id | STRING | TEXT  |
| role | STRING | TEXT  |
| email | STRING | TEXT  |
| phone | STRING | TEXT  |
| name | STRING | TEXT  |






### Get Contact
Get a single Contact

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Contact ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| type | STRING | TEXT  |
| id | STRING | TEXT  |
| role | STRING | TEXT  |
| email | STRING | TEXT  |
| phone | STRING | TEXT  |
| name | STRING | TEXT  |






### Send Message
Send a new message

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| message_type | Message Type | STRING | SELECT  |  In app message or email message  |  true  |
| subject | Title | STRING | TEXT  |  Title of the Email/Message  |  true  |
| body | Content | STRING | RICH_TEXT  |  Content of the message  |  true  |
| template | Template | STRING | SELECT  |  The style of the outgoing message  |  true  |
| to | To | STRING | SELECT  |  ID of the contact to send the message to.  |  true  |


#### Output



Type: OBJECT









