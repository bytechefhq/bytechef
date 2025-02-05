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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Customer
Creates a new customer.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Customer | {STRING\(email), STRING\(name), STRING\(description), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(description), STRING\(email), STRING\(name), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} | OBJECT_BUILDER  |






### Create Invoice
Creates a new invoice.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Invoice | {STRING\(customer), STRING\(currency), STRING\(description)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id), STRING\(customer), STRING\(currency), STRING\(description)} | OBJECT_BUILDER  |








## Triggers


### New Customer
Triggers when a new customer is created.

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
| id | STRING | TEXT  |
| object | STRING | TEXT  |
| description | STRING | TEXT  |
| email | STRING | TEXT  |
| name | STRING | TEXT  |
| phone | STRING | TEXT  |
| address | {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} | OBJECT_BUILDER  |







### New Invoice
Triggers on a new invoice.

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
| id | STRING | TEXT  |
| object | STRING | TEXT  |
| currency | STRING | TEXT  |
| customer | STRING | TEXT  |
| customer_name | STRING | TEXT  |
| description | STRING | TEXT  |







<hr />

