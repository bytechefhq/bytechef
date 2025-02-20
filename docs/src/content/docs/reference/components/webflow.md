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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Fulfill Order
Name: fulfillOrder

Updates an order's status to fulfilled.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING | SELECT |  | true |
| orderId | Order ID | STRING <details> <summary> Depends On </summary> siteId </details> | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(orderId), STRING\(status)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| siteId | Site ID | STRING | SELECT |  | true |
| collectionId | Collection ID | STRING <details> <summary> Depends On </summary> siteId </details> | SELECT |  | true |
| itemId | Item  ID | STRING <details> <summary> Depends On </summary> collectionId, siteId </details> | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), {STRING\(name), STRING\(slug)}\(fieldData)} </details> | OBJECT_BUILDER |




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




