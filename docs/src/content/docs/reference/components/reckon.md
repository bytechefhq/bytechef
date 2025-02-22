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
| __item | Contact | OBJECT <details> <summary> Properties </summary> {STRING\(name)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id)} </details> |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "bookId" : "",
    "__item" : {
      "name" : ""
    }
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
| __item | Invoice | OBJECT <details> <summary> Properties </summary> {STRING\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id)} </details> |




#### JSON Example
```json
{
  "label" : "Create Invoice",
  "name" : "createInvoice",
  "parameters" : {
    "bookId" : "",
    "__item" : {
      "customer" : "",
      "invoiceDate" : "2021-01-01",
      "amountTaxStatus" : "",
      "lineItems" : [ {
        "lineNumber" : 1
      } ]
    }
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
| __item | Payment | OBJECT <details> <summary> Properties </summary> {STRING\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(id)} </details> |




#### JSON Example
```json
{
  "label" : "Create Payment",
  "name" : "createPayment",
  "parameters" : {
    "bookId" : "",
    "__item" : {
      "supplier" : "",
      "paymentDate" : "2021-01-01",
      "totalAmount" : 0.0
    }
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

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(invoiceNumber), {STRING\(id), STRING\(name)}\(customer), DATE\(invoiceDate), STRING\(amountTaxStatus), [{INTEGER\(lineNumber)}]\(lineItems)} </details> |




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

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(paymentNumber), {STRING\(id), STRING\(name)}\(supplier), DATE\(paymentDate), NUMBER\(totalAmount)} </details> |




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

