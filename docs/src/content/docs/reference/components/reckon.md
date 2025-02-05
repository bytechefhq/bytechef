---
title: "Reckon"
description: "Reckon is an accounting software used for financial management and bookkeeping tasks."
---

Reckon is an accounting software used for financial management and bookkeeping tasks.


Categories: accounting


Type: reckon/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Contact
Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| bookId | Book ID | STRING | SELECT  |  ID of the book where new contact will be created.  |  true  |
| __item | Contact | {STRING\(name)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id)} | OBJECT_BUILDER  |






### Create Invoice
Creates a new invoice.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| bookId | Book ID | STRING | SELECT  |  ID of the book where new invoice will be created.  |  true  |
| __item | Invoice | {STRING\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id)} | OBJECT_BUILDER  |






### Create Payment
Creates a new payment.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| bookId | Book ID | STRING | SELECT  |  ID of the book where new payment will be created.  |  true  |
| __item | Payment | {STRING\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(id)} | OBJECT_BUILDER  |








## Triggers


### New Invoice
Triggers when a new invoice is created.

Type: POLLING
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| bookId | Book | STRING | SELECT  |  | true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(invoiceNumber), {STRING\(id), STRING\(name)}\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} | OBJECT_BUILDER  |







### New Payment
Triggers when a new payment is created.

Type: POLLING
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| bookId | Book | STRING | SELECT  |  | true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {STRING\(id), STRING\(paymentNumber), {STRING\(id), STRING\(name)}\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} | OBJECT_BUILDER  |







<hr />

