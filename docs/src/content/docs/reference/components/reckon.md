---
title: "Reckon"
description: "Reckon is an accounting software used for financial management and bookkeeping tasks."
---
## Reference
<hr />

Reckon is an accounting software used for financial management and bookkeeping tasks.


Categories: [ACCOUNTING]


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


### Create Contact
Creates a new Contact.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Book | STRING | SELECT  |  Book where new contact will be created.  |
| Contact | {STRING\(name)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id)} | OBJECT_BUILDER  |






### Create Invoice
Create a new Invoice.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Book | STRING | SELECT  |  Book where new invoice will be created.  |
| Invoice | {STRING\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id)} | OBJECT_BUILDER  |






### Create Payment
Creates a new payment.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Book | STRING | SELECT  |  Book where new payment will be created.  |
| Payment | {STRING\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id)} | OBJECT_BUILDER  |






