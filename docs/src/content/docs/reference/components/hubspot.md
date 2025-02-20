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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| hapikey | Hubspot API Key | STRING | TEXT | API Key is used for registering webhooks. | false |





<hr />



## Actions


### Create Contact
Name: createContact

Create a contact with the given properties.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {{STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
      "properties" : {
        "firstname" : "",
        "lastname" : "",
        "email" : "",
        "phone" : "",
        "company" : "",
        "website" : ""
      }
    }
  },
  "type" : "hubspot/v1/createContact"
}
```


### Delete Contact
Name: deleteContact

Move Contact to the recycling bin.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| contactId | Contact ID | STRING | SELECT |  | true |


#### JSON Example
```json
{
  "label" : "Delete Contact",
  "name" : "deleteContact",
  "parameters" : {
    "contactId" : ""
  },
  "type" : "hubspot/v1/deleteContact"
}
```


### Get Contact
Name: getContact

Get contact details.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| contactId | Contact ID | STRING | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Contact",
  "name" : "getContact",
  "parameters" : {
    "contactId" : ""
  },
  "type" : "hubspot/v1/getContact"
}
```


### Update Contact
Name: updateContact

Update Contact properties.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| contactId | Contact | STRING | SELECT |  | true |
| __item | Contact | OBJECT <details> <summary> Properties </summary> {{STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(firstname), STRING\(lastname), STRING\(email), STRING\(phone), STRING\(company), STRING\(website)}\(properties)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Update Contact",
  "name" : "updateContact",
  "parameters" : {
    "contactId" : "",
    "__item" : {
      "properties" : {
        "firstname" : "",
        "lastname" : "",
        "email" : "",
        "phone" : "",
        "company" : "",
        "website" : ""
      }
    }
  },
  "type" : "hubspot/v1/updateContact"
}
```


### Create Deal
Name: createDeal

Creates a new deal.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Deal | OBJECT <details> <summary> Properties </summary> {{STRING\(dealname), NUMBER\(amount), DATE\(closedate), STRING\(pipeline), STRING\(dealstage), STRING\(hubspot_owner_id)}\(properties)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(dealname), NUMBER\(amount), DATE\(closedate), STRING\(pipeline), STRING\(dealstage), STRING\(hubspot_owner_id)}\(properties)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Deal",
  "name" : "createDeal",
  "parameters" : {
    "__item" : {
      "properties" : {
        "dealname" : "",
        "amount" : 0.0,
        "closedate" : "2021-01-01",
        "pipeline" : "",
        "dealstage" : "",
        "hubspot_owner_id" : ""
      }
    }
  },
  "type" : "hubspot/v1/createDeal"
}
```


### Get Ticket
Name: getTicket

Gets ticket details.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| ticketId | Ticket ID | STRING | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(content), STRING\(hs_object_id), STRING\(hs_pipeline), STRING\(hs_pipeline_stage), STRING\(hs_ticket_priority), STRING\(subject)}\(properties)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Ticket",
  "name" : "getTicket",
  "parameters" : {
    "ticketId" : ""
  },
  "type" : "hubspot/v1/getTicket"
}
```




## Triggers


### New Contact
Name: newContact

Triggers when new contact is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| appId | App Id | STRING | TEXT | The id of a Hubspot app used to register this trigger to. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| eventId | STRING | TEXT |
| subscriptionId | STRING | TEXT |
| subscriptionType | STRING | TEXT |
| objectId | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Contact",
  "name" : "newContact",
  "parameters" : {
    "appId" : ""
  },
  "type" : "hubspot/v1/newContact"
}
```


### New Deal
Name: newDeal

Triggers when a new deal is added.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| appId | App Id | STRING | TEXT | The id of a Hubspot app used to register this trigger to. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| eventId | STRING | TEXT |
| subscriptionId | STRING | TEXT |
| subscriptionType | STRING | TEXT |
| objectId | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Deal",
  "name" : "newDeal",
  "parameters" : {
    "appId" : ""
  },
  "type" : "hubspot/v1/newDeal"
}
```


### New Ticket
Name: newTicket

Triggers when new ticket is created.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| appId | App Id | STRING | TEXT | The id of a Hubspot app used to register this trigger to. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| eventId | STRING | TEXT |
| subscriptionId | STRING | TEXT |
| subscriptionType | STRING | TEXT |
| objectId | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Ticket",
  "name" : "newTicket",
  "parameters" : {
    "appId" : ""
  },
  "type" : "hubspot/v1/newTicket"
}
```


<hr />

