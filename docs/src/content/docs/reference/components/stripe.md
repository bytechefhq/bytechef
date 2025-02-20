---
title: "Stripe"
description: "Stripe is a payment processing platform that allows businesses to accept online payments and manage transactions securely."
---

Stripe is a payment processing platform that allows businesses to accept online payments and manage transactions securely.


Categories: payment-processing


Type: stripe/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | Token | STRING | TEXT |  | true |





<hr />



## Actions


### Create Customer
Name: createCustomer

Creates a new customer.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Customer | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(name), STRING\(description), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(description), STRING\(email), STRING\(name), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Customer",
  "name" : "createCustomer",
  "parameters" : {
    "__item" : {
      "email" : "",
      "name" : "",
      "description" : "",
      "phone" : "",
      "address" : {
        "city" : "",
        "country" : "",
        "line1" : "",
        "line2" : "",
        "postal_code" : "",
        "state" : ""
      }
    }
  },
  "type" : "stripe/v1/createCustomer"
}
```


### Create Invoice
Name: createInvoice

Creates a new invoice.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Invoice | OBJECT <details> <summary> Properties </summary> {STRING\(customer), STRING\(currency), STRING\(description)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(customer), STRING\(currency), STRING\(description)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Invoice",
  "name" : "createInvoice",
  "parameters" : {
    "__item" : {
      "customer" : "",
      "currency" : "",
      "description" : ""
    }
  },
  "type" : "stripe/v1/createInvoice"
}
```




## Triggers


### New Customer
Name: newCustomer

Triggers when a new customer is created.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| object | STRING | TEXT |
| description | STRING | TEXT |
| email | STRING | TEXT |
| name | STRING | TEXT |
| phone | STRING | TEXT |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "New Customer",
  "name" : "newCustomer",
  "type" : "stripe/v1/newCustomer"
}
```


### New Invoice
Name: newInvoice

Triggers on a new invoice.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| object | STRING | TEXT |
| currency | STRING | TEXT |
| customer | STRING | TEXT |
| customer_name | STRING | TEXT |
| description | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Invoice",
  "name" : "newInvoice",
  "type" : "stripe/v1/newInvoice"
}
```


<hr />

