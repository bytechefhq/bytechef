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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| shopName | Shop name | STRING |  | true |
| key | Access token | STRING |  | true |
| value | Access Token | STRING |  | true |





<hr />



## Actions


### Create Order
Name: createOrder

Adds an order into a Shopify store.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Order | OBJECT <details> <summary> Properties </summary> {{[{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}]\(line_items), STRING\(total_tax), STRING\(currency)}\(order)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "Create Order",
  "name" : "createOrder",
  "parameters" : {
    "__item" : {
      "order" : {
        "line_items" : [ {
          "fulfillment_status" : "",
          "grams" : "",
          "price" : 0.0,
          "product_id" : 1,
          "variant_id" : 1,
          "quantity" : 1,
          "title" : ""
        } ],
        "total_tax" : "",
        "currency" : ""
      }
    }
  },
  "type" : "shopify/v1/createOrder"
}
```


### Delete Order
Name: deleteOrder

Deletes an order. Orders that interact with an online gateway can't be deleted.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| orderId | Order ID | INTEGER | ID of the order to delete. | true |


#### JSON Example
```json
{
  "label" : "Delete Order",
  "name" : "deleteOrder",
  "parameters" : {
    "orderId" : 1
  },
  "type" : "shopify/v1/deleteOrder"
}
```


### Cancel an order
Name: cancelOrder

Cancels an order. Orders that are paid and have fulfillments can't be canceled.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| orderId | Order ID | INTEGER | ID of the order to cancel. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "Cancel an order",
  "name" : "cancelOrder",
  "parameters" : {
    "orderId" : 1
  },
  "type" : "shopify/v1/cancelOrder"
}
```


### Update Order
Name: updateOrder

Update an existing order.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| orderId | Order ID | INTEGER | ID of the order to update. | true |
| __item | Order | OBJECT <details> <summary> Properties </summary> {{STRING\(note), STRING\(email), STRING\(phone), STRING\(tags)}\(order)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "Update Order",
  "name" : "updateOrder",
  "parameters" : {
    "orderId" : 1,
    "__item" : {
      "order" : {
        "note" : "",
        "email" : "",
        "phone" : "",
        "tags" : ""
      }
    }
  },
  "type" : "shopify/v1/updateOrder"
}
```


### Close Order
Name: closeOrder

Closes an order. A closed order is one that has no more work to be done. All items have been fulfilled or refunded.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| orderId | Order ID | INTEGER | ID of the order to close. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "Close Order",
  "name" : "closeOrder",
  "parameters" : {
    "orderId" : 1
  },
  "type" : "shopify/v1/closeOrder"
}
```




## Triggers


### New Cancelled Order
Name: newCancelledOrder

Triggers when order is cancelled.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "New Cancelled Order",
  "name" : "newCancelledOrder",
  "type" : "shopify/v1/newCancelledOrder"
}
```


### New Order
Name: newOrder

Triggers when new order is created.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "New Order",
  "name" : "newOrder",
  "type" : "shopify/v1/newOrder"
}
```


### New Paid Order
Name: newPaidOrder

Triggers when paid order is created.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| currency | STRING |
| note | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| tags | STRING |
| line_items | ARRAY <details> <summary> Items </summary> [{STRING\(fulfillment_status), STRING\(grams), NUMBER\(price), INTEGER\(product_id), INTEGER\(variant_id), INTEGER\(quantity), STRING\(title)}] </details> |




#### JSON Example
```json
{
  "label" : "New Paid Order",
  "name" : "newPaidOrder",
  "type" : "shopify/v1/newPaidOrder"
}
```


<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(52.85156250% + 32px)"><iframe src="https://www.guidejar.com/embed/11b2d3f5-3d31-40af-be00-c5845ab71165?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
