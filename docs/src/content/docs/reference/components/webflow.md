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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Fulfill Order
Updates an order's status to fulfilled.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteId | Site ID | STRING | SELECT  |  | true  |
| orderId | Order ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(orderId), STRING\(status)} | OBJECT_BUILDER  |






### Get Collection Item
Get collection item in a collection.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| siteId | Site ID | STRING | SELECT  |  | true  |
| collectionId | Collection ID | STRING | SELECT  |    |  true  |
| itemId | Item  ID | STRING | SELECT  |    |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), {STRING\(name), STRING\(slug)}\(fieldData)} | OBJECT_BUILDER  |








## Triggers



<hr />

