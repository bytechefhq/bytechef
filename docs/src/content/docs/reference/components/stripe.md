---
title: "Stripe"
description: "Stripe is a payment processing platform that allows businesses to accept online payments and manage transactions securely."
---
## Reference
<hr />

Stripe is a payment processing platform that allows businesses to accept online payments and manage transactions securely.


Categories: [payment-processing]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |





<hr />



## Triggers


### New Customer
Triggers when a new customer is created.

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
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)} | OBJECT_BUILDER  |







### New Invoice
Triggers on a new invoice.

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
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |







<hr />



## Actions


### Create Customer
Creates a new customer.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Customer | {STRING\(email), STRING\(name), STRING\(description), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(description), STRING\(email), STRING\(name), STRING\(phone), {STRING\(city), STRING\(country), STRING\(line1), STRING\(line2), STRING\(postal_code), STRING\(state)}\(address)} | OBJECT_BUILDER  |






### Create Invoice
Creates a new invoice.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Invoice | {STRING\(customer), STRING\(currency), STRING\(description)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(customer), STRING\(currency), STRING\(description)} | OBJECT_BUILDER  |






