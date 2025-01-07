---
title: "Hubspot"
description: "HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service."
---
## Reference
<hr />

HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service.


Categories: [marketing-automation]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Hubspot API Key | STRING | TEXT  |  API Key is used for registering webhooks.  |





<hr />



## Triggers


### New Contact
Triggers when new contact is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |







### New Deal
Triggers when a new deal is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |







### New Ticket
Triggers when new ticket is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |







<hr />



## Actions


### Create Contact
Create a contact with the given properties.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | {{STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |






### Delete Contact
Move Contact to the recycling bin.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | STRING | SELECT  |  |




### Get Contact
Get contact details.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |






### Update Contact
Update Contact properties.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | STRING | SELECT  |  |
| Contact | {{STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} | OBJECT_BUILDER  |






### Create Deal
Creates a new deal.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Deal | {{STRING\(dealname), NUMBER\(amount), DATE\(closedate), STRING\(pipeline), STRING\(dealstage), STRING\(hubspot_owner_id)}\(properties)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), {STRING\(dealname), NUMBER\(amount), DATE\(closedate), STRING\(pipeline), STRING\(dealstage), STRING\(hubspot_owner_id)}\(properties)} | OBJECT_BUILDER  |






### Get Ticket
Gets ticket details.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Ticket ID | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), {STRING\(content), STRING\(hs_object_id), STRING\(hs_pipeline), STRING\(hs_pipeline_stage), STRING\(hs_ticket_priority), STRING\(subject)}\(properties)} | OBJECT_BUILDER  |






