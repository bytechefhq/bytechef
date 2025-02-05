---
title: "Shopify"
description: "Shopify is an e-commerce platform that allows businesses to create online stores and sell products."
---

Shopify is an e-commerce platform that allows businesses to create online stores and sell products.


Categories: e-commerce


Type: shopify/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| shopName | Shop name | STRING | TEXT  |  | true  |
| key | Access token | STRING | TEXT  |  | true  |
| value | Access Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Order
Adds an order into a Shopify store.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Order | {{[{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}]\(line_items), STRING\(total_tax), STRING\(currency)}\(order)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |






### Delete Order
Deletes an order. Orders that interact with an online gateway can't be deleted.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| orderId | Order ID | INTEGER | SELECT  |  ID of the order to delete.  |  true  |




### Cancel an order
Cancels an order. Orders that are paid and have fulfillments can't be canceled.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| orderId | Order ID | INTEGER | SELECT  |  ID of the order to cancel.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |






### Update Order
Update an existing order.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| orderId | Order ID | INTEGER | SELECT  |  ID of the order to update.  |  true  |
| __item | Order | {{STRING\(note), STRING\(email), STRING\(phone), STRING\(tags)}\(order)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |






### Close Order
Closes an order. A closed order is one that has no more work to be done. All items have been fulfilled or refunded.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| orderId | Order ID | INTEGER | SELECT  |  ID of the order to close.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |








## Triggers


### New Cancelled Order
Triggers when order is cancelled.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |







### New Order
Triggers when new order is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |







### New Paid Order
Triggers when paid order is created.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| currency | Currency | STRING | TEXT  |  | false  |
| note | Note | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| name | Name | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| tags | Tags | STRING | TEXT  |  | false  |
| line_items | Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  | false  |







<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.85156250% + 32px)"><iframe src="https://www.guidejar.com/embed/11b2d3f5-3d31-40af-be00-c5845ab71165?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
