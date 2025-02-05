---
title: "Hubspot"
description: "HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service."
---

HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service.


Categories: marketing-automation


Type: hubspot/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| hapikey | Hubspot API Key | STRING | TEXT  |  API Key is used for registering webhooks.  |  false  |





<hr />



## Actions


### Create Contact
Create a contact with the given properties.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Contact | {{STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |






### Delete Contact
Move Contact to the recycling bin.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| contactId | Contact ID | STRING | SELECT  |  | true  |




### Get Contact
Get contact details.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| contactId | Contact ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |






### Update Contact
Update Contact properties.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| contactId | Contact | STRING | SELECT  |  | true  |
| __item | Contact | {{STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |






### Create Deal
Creates a new deal.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Deal | {{STRING\(dealname), NUMBER\(amount), DATE\(closedate), STRING\(pipeline), STRING\(dealstage), STRING\(hubspot_owner_id)}\(properties)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), {STRING\(dealname), NUMBER\(amount), DATE\(closedate), STRING\(pipeline), STRING\(dealstage), STRING\(hubspot_owner_id)}\(properties)} | OBJECT_BUILDER  |






### Get Ticket
Gets ticket details.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| ticketId | Ticket ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), {STRING\(content), STRING\(hs_object_id), STRING\(hs_pipeline), STRING\(hs_pipeline_stage), STRING\(hs_ticket_priority), STRING\(subject)}\(properties)} | OBJECT_BUILDER  |








## Triggers


### New Contact
Triggers when new contact is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| appId | App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| eventId | STRING | TEXT  |
| subscriptionId | STRING | TEXT  |
| subscriptionType | STRING | TEXT  |
| objectId | STRING | TEXT  |







### New Deal
Triggers when a new deal is added.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| appId | App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| eventId | STRING | TEXT  |
| subscriptionId | STRING | TEXT  |
| subscriptionType | STRING | TEXT  |
| objectId | STRING | TEXT  |







### New Ticket
Triggers when new ticket is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| appId | App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| eventId | STRING | TEXT  |
| subscriptionId | STRING | TEXT  |
| subscriptionType | STRING | TEXT  |
| objectId | STRING | TEXT  |







<hr />

