---
title: "Intercom"
description: "Intercom is the complete AI-first customer service solution, giving exceptional experiences for support teams with AI agent, AI copilot, tickets, ..."
---
## Reference
<hr />

Intercom is the complete AI-first customer service solution, giving exceptional experiences for support teams with AI agent, AI copilot, tickets, ...


Categories: [CUSTOMER_SUPPORT]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client id | STRING | TEXT  |  |
| Client secret | STRING | TEXT  |  |





<hr />





## Actions


### Create Contact
Create new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact Role | STRING | SELECT  |  Role of the contact  |
| Contact Email | STRING | TEXT  |  Email of the contact  |
| Contact Name | STRING | TEXT  |  Name of the contact  |
| Contact Phone | STRING | TEXT  |  Phone of the contact must start with a "+" sign  |
| Contact Image | STRING | TEXT  |  Image of the contact  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Get Contact
Get a single Contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact Name | STRING | SELECT  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Send Message
Send a new message

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Message Type | STRING | SELECT  |  In app message or email message  |
| Title | STRING | TEXT  |  Title of the Email/Message  |
| Content | STRING | TEXT  |  Content of the message  |
| Template | STRING | SELECT  |  The style of the outgoing message  |
| To | STRING | SELECT  |  Receiver of the message  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





