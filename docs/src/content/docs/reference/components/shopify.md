---
title: "Shopify"
description: "Shopify is an e-commerce platform that allows businesses to create online stores and sell products."
---
## Reference
<hr />

Shopify is an e-commerce platform that allows businesses to create online stores and sell products.


Categories: [e-commerce]


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


### New Cancelled Order
Triggers when order is cancelled.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |







### New Order
Triggers when new order is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |







### New Paid Order
Triggers when paid order is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |







<hr />



## Actions


### Create Order
Adds an order into a Shopify store.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order | {{[{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}]\(line_items), STRING\(total_tax), STRING\(currency)}\(order)} | OBJECT_BUILDER  |  |


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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |






### Delete Order
Deletes an order. Orders that interact with an online gateway can't be deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order | INTEGER | SELECT  |  The order to delete.  |




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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |






### Update Order
Update an existing order.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order | INTEGER | SELECT  |  The order to update.  |
| Order | {{STRING\(note), STRING\(email), STRING\(phone), STRING\(tags)}\(order)} | OBJECT_BUILDER  |  |


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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |






### Close Order
Closes an order. A closed order is one that has no more work to be done. All items have been fulfilled or refunded.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order | INTEGER | SELECT  |  The order to close.  |


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
| Line Items | [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] | ARRAY_BUILDER  |  |






<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.85156250% + 32px)"><iframe src="https://www.guidejar.com/embed/11b2d3f5-3d31-40af-be00-c5845ab71165?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
