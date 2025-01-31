---
title: "Webflow"
description: "Webflow is a web design and development platform that allows users to build responsive websites visually without writing code."
---
## Reference
<hr />

Webflow is a web design and development platform that allows users to build responsive websites visually without writing code.


Categories: [developer-tools]


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



<hr />



## Actions


### Fulfill Order
Updates an order's status to fulfilled.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site ID | STRING | SELECT  |  |
| Order ID | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(orderId), STRING\(status)} | OBJECT_BUILDER  |






### Get Collection Item
Get collection item in a collection.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Site ID | STRING | SELECT  |  |
| Collection ID | STRING | SELECT  |    |
| Item  ID | STRING | SELECT  |    |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), {STRING\(name), STRING\(slug)}\(fieldData)} | OBJECT_BUILDER  |






