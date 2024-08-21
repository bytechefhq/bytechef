---
title: "Hubspot"
description: "HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service."
---
## Reference
<hr />

HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service.


Categories: [MARKETING_AUTOMATION]


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





<hr />



## Triggers


### Subscribe
Triggers when an event of the subscribed type happens inside HubSpot.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| App Id | STRING | TEXT  |  The id of a Hubspot app used to register this trigger to. See the <a href="https://legacydocs.hubspot.com/docs/faq/integration-platform-api-requirements">prerequisites documentation</a> for more details about creating an app  |
| Event Type | STRING | SELECT  |  The list of available event types for which you want to receive events.  |
| Property Name | STRING | TEXT  |  The name of property to listen for change events.  |


### Output


___Sample Output:___

```[
    {
        "objectId": 1246965,
        "propertyName": "lifecyclestage",
        "propertyValue": "subscriber",
        "changeSource": "ACADEMY",
        "eventId": 3816279340,
        "subscriptionId": 25,
        "portalId": 33,
        "appId": 1160452,
        "occurredAt": 1462216307945,
        "eventType":"contact.propertyChange",
        "attemptNumber": 0
   }
]
```



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(objectId), STRING\(propertyName), STRING\(propertyValue), STRING\(changeSource), INTEGER\(eventId), INTEGER\(subscriptionId), INTEGER\(portalId), INTEGER\(appId), INTEGER\(occurredAt), STRING\(eventType), INTEGER\(attemptNumber), INTEGER\(messageId), STRING\(messageType)}\(data)} | OBJECT_BUILDER  |







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
| Contact   To   Update . | STRING | SELECT  |  |
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






