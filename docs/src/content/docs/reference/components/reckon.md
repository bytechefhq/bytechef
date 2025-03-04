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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| bookId | Book ID | STRING | ID of the book where new contact will be created. | true |
| name | Name | STRING | The name of the contact. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "bookId" : "",
    "name" : ""
  },
  "type" : "reckon/v1/createContact"
}
```


### Create Invoice
Name: createInvoice

Creates a new invoice.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| bookId | Book ID | STRING | ID of the book where new invoice will be created. | true |
| customer | Customer | STRING | The customer that is being invoiced. | true |
| invoiceDate | Invoice Date | DATE | The date of the invoice. | true |
| amountTaxStatus | Amount Tax Status | STRING <details> <summary> Options </summary> NonTaxed, Inclusive, Exclusive </details> | The amount tax status of the amounts in the invoice. | true |
| lineItems | Line Items | ARRAY <details> <summary> Items </summary> [{INTEGER\(lineNumber)}] </details> | The individual items that make up the invoice. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Invoice",
  "name" : "createInvoice",
  "parameters" : {
    "bookId" : "",
    "customer" : "",
    "invoiceDate" : "2021-01-01",
    "amountTaxStatus" : "",
    "lineItems" : [ {
      "lineNumber" : 1
    } ]
  },
  "type" : "reckon/v1/createInvoice"
}
```


### Create Payment
Name: createPayment

Creates a new payment.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| bookId | Book ID | STRING | ID of the book where new payment will be created. | true |
| supplier | Supplier | STRING | The supplier that is being paid. | true |
| paymentDate | Payment Date | DATE | The date of the payment. | true |
| totalAmount | Total Amount | NUMBER | The total amount of the payment applied. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Payment",
  "name" : "createPayment",
  "parameters" : {
    "bookId" : "",
    "supplier" : "",
    "paymentDate" : "2021-01-01",
    "totalAmount" : 0.0
  },
  "type" : "reckon/v1/createPayment"
}
```




## Triggers


### New Invoice
Name: newInvoice

Triggers when a new invoice is created.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| bookId | Book | STRING |  | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(invoiceNumber), {STRING\(id), STRING\(name)}\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} </details> |  |




#### JSON Example
```json
{
  "label" : "New Invoice",
  "name" : "newInvoice",
  "parameters" : {
    "bookId" : ""
  },
  "type" : "reckon/v1/newInvoice"
}
```


### New Payment
Name: newPayment

Triggers when a new payment is created.

Type: POLLING

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| bookId | Book | STRING |  | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(paymentNumber), {STRING\(id), STRING\(name)}\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} </details> |  |




#### JSON Example
```json
{
  "label" : "New Payment",
  "name" : "newPayment",
  "parameters" : {
    "bookId" : ""
  },
  "type" : "reckon/v1/newPayment"
}
```


<hr />

