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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| token | Token | STRING |  | true |





<hr />



## Actions


### Create Customer
Name: createCustomer

Creates a new customer.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Customer | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(name), STRING\(description), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(description), STRING\(email), STRING\(name), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} </details> |




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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Invoice | OBJECT <details> <summary> Properties </summary> {STRING\(customer), STRING\(currency), STRING\(description)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(customer), STRING\(currency), STRING\(description)} </details> |




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

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| object | STRING |
| description | STRING |
| email | STRING |
| name | STRING |
| phone | STRING |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} </details> |




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

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| object | STRING |
| currency | STRING |
| customer | STRING |
| customer_name | STRING |
| description | STRING |




#### JSON Example
```json
{
  "label" : "New Invoice",
  "name" : "newInvoice",
  "type" : "stripe/v1/newInvoice"
}
```


<hr />

