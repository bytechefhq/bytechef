---
title: "Webflow"
description: "Webflow is a web design and development platform that allows users to build responsive websites visually without writing code."
---

Webflow is a web design and development platform that allows users to build responsive websites visually without writing code.


Categories: developer-tools


Type: webflow/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Fulfill Order
Name: fulfillOrder

Updates an order's status to fulfilled.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING |  | true |
| orderId | Order ID | STRING <details> <summary> Depends On </summary> siteId </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(orderId), STRING\(status)} </details> |




#### JSON Example
```json
{
  "label" : "Fulfill Order",
  "name" : "fulfillOrder",
  "parameters" : {
    "siteId" : "",
    "orderId" : ""
  },
  "type" : "webflow/v1/fulfillOrder"
}
```


### Get Collection Item
Name: getCollectionItem

Get collection item in a collection.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING |  | true |
| collectionId | Collection ID | STRING <details> <summary> Depends On </summary> siteId </details> |  | true |
| itemId | Item  ID | STRING <details> <summary> Depends On </summary> collectionId, siteId </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(name), STRING\(slug)}\(fieldData)} </details> |




#### JSON Example
```json
{
  "label" : "Get Collection Item",
  "name" : "getCollectionItem",
  "parameters" : {
    "siteId" : "",
    "collectionId" : "",
    "itemId" : ""
  },
  "type" : "webflow/v1/getCollectionItem"
}
```




