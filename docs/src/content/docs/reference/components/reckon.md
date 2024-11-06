---
title: "Reckon"
description: "Reckon is an accounting software used for financial management and bookkeeping tasks."
---
## Reference
<hr />

Reckon is an accounting software used for financial management and bookkeeping tasks.


Categories: [accounting]


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


### New Invoice
Triggers when a new invoice is created.

#### Type: POLLING
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Book | STRING | SELECT  |  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(invoiceNumber), {STRING\(id), STRING\(name)}\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} | OBJECT_BUILDER  |







### New Payment
Triggers when a new payment is created.

#### Type: POLLING
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Book | STRING | SELECT  |  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(paymentNumber), {STRING\(id), STRING\(name)}\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} | OBJECT_BUILDER  |







<hr />



## Actions


### Create Contact
Creates a new contact.

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
Creates a new invoice.

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






