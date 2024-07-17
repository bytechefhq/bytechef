---
title: "Shopify"
description: "Shopify is an e-commerce platform that allows businesses to create online stores and sell products."
---
## Reference
<hr />

Shopify is an e-commerce platform that allows businesses to create online stores and sell products.


Categories: [E_COMMERCE]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Shop name | STRING | TEXT  |  |
| Access token | STRING | TEXT  |  |
| Access Token | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create an order
Adds an order into a Shopify store.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order | {{[{STRING(fulfillment_status), STRING(grams), NUMBER(price), INTEGER(product_id), INTEGER(variant_id), INTEGER(quantity), STRING(title)}](line_items), STRING(total_tax), STRING(currency)}(order)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Currency | STRING | TEXT  |  |
| Note | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Name | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| Tags | STRING | TEXT  |  |
| Line Items | [{STRING(fulfillment_status), STRING(grams), NUMBER(price), INTEGER(product_id), INTEGER(variant_id), INTEGER(quantity), STRING(title)}] | ARRAY_BUILDER  |  |





### Delete an order
Deletes an order. Orders that interact with an online gateway can't be deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order Id | INTEGER | SELECT  |  The order id.  |




### Cancel an order
Cancels an order. Orders that are paid and have fulfillments can't be canceled.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order Id | INTEGER | SELECT  |  The order id.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Currency | STRING | TEXT  |  |
| Note | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Name | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| Tags | STRING | TEXT  |  |
| Line Items | [{STRING(fulfillment_status), STRING(grams), NUMBER(price), INTEGER(product_id), INTEGER(variant_id), INTEGER(quantity), STRING(title)}] | ARRAY_BUILDER  |  |





### Update an order
Update an existing order.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order Id | INTEGER | SELECT  |  The order id.  |
| Order | {{STRING(note), STRING(email), STRING(phone), STRING(tags)}(order)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Currency | STRING | TEXT  |  |
| Note | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Name | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| Tags | STRING | TEXT  |  |
| Line Items | [{STRING(fulfillment_status), STRING(grams), NUMBER(price), INTEGER(product_id), INTEGER(variant_id), INTEGER(quantity), STRING(title)}] | ARRAY_BUILDER  |  |





### Close an order
Closes an order. A closed order is one that has no more work to be done. All items have been fulfilled or refunded.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order Id | INTEGER | SELECT  |  The order id.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Currency | STRING | TEXT  |  |
| Note | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Name | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| Tags | STRING | TEXT  |  |
| Line Items | [{STRING(fulfillment_status), STRING(grams), NUMBER(price), INTEGER(product_id), INTEGER(variant_id), INTEGER(quantity), STRING(title)}] | ARRAY_BUILDER  |  |





