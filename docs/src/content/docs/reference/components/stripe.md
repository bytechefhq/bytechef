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
| email | Email | STRING | Customer’s email address. | false |
| name | Name | STRING | The customer's full name. | false |
| description | Description | STRING |  | false |
| phone | Phone | STRING |  | false |
| address | Address | OBJECT <details> <summary> Properties </summary> {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} </details> |  | false |

#### Example JSON Structure
```json
{
  "label" : "Create Customer",
  "name" : "createCustomer",
  "parameters" : {
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
  },
  "type" : "stripe/v1/createCustomer"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| description | STRING |  |
| email | STRING |  |
| name | STRING |  |
| phone | STRING |  |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} </details> |  |




#### Output Example
```json
{
  "id" : "",
  "description" : "",
  "email" : "",
  "name" : "",
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
```


### Create Invoice
Name: createInvoice

Creates a new invoice.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| customer | Customer ID | STRING | ID of the customer who will be billed. | true |
| currency | Currency | STRING | Currency used for invoice. | true |
| description | Description | STRING | Description for the invoice. | false |

#### Example JSON Structure
```json
{
  "label" : "Create Invoice",
  "name" : "createInvoice",
  "parameters" : {
    "customer" : "",
    "currency" : "",
    "description" : ""
  },
  "type" : "stripe/v1/createInvoice"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| customer | STRING |  |
| currency | STRING |  |
| description | STRING |  |




#### Output Example
```json
{
  "id" : "",
  "customer" : "",
  "currency" : "",
  "description" : ""
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

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| object | STRING |  |
| description | STRING |  |
| email | STRING |  |
| name | STRING |  |
| phone | STRING |  |
| address | OBJECT <details> <summary> Properties </summary> {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} </details> |  |




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

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |
| object | STRING |  |
| currency | STRING |  |
| customer | STRING |  |
| customer_name | STRING |  |
| description | STRING |  |




#### JSON Example
```json
{
  "label" : "New Invoice",
  "name" : "newInvoice",
  "type" : "stripe/v1/newInvoice"
}
```


<hr />

