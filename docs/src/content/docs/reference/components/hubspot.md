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
null






<hr />



## Actions


### Create
Create a contact with the given properties and return a copy of the object, including the ID. Documentation and examples for creating standard contacts is provided.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Simple Public Object Input For Create | {{STRING(company), STRING(email), STRING(firstname), STRING(lastname), STRING(phone), STRING(website)}(properties), [{{STRING(id)}(to), [{STRING(associationCategory), INTEGER(associationTypeId)}](types)}](associations)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | STRING | TEXT  |  |
| Properties | {} | OBJECT_BUILDER  |  |
| Properties With History | {} | OBJECT_BUILDER  |  |
| Created At | DATE_TIME | DATE_TIME  |  |
| Updated At | DATE_TIME | DATE_TIME  |  |
| Archived | BOOLEAN | SELECT  |  |
| Archived At | DATE_TIME | DATE_TIME  |  |





### Update
Perform a partial update of an Object identified by `{contactId}`. `{contactId}` refers to the internal object ID. Provided property values will be overwritten. Read-only and non-existent properties will be ignored. Properties values can be cleared by passing an empty string.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact Id | STRING | TEXT  |  |
| Simple Public Object Input | {{STRING(company), STRING(email), STRING(firstname), STRING(lastname), STRING(phone), STRING(website)}(properties)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | STRING | TEXT  |  |
| Properties | {} | OBJECT_BUILDER  |  |
| Properties With History | {} | OBJECT_BUILDER  |  |
| Created At | DATE_TIME | DATE_TIME  |  |
| Updated At | DATE_TIME | DATE_TIME  |  |
| Archived | BOOLEAN | SELECT  |  |
| Archived At | DATE_TIME | DATE_TIME  |  |





