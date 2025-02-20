---
title: "QuickBooks"
description: "QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly toward small and medium-sized businesses and offers on-premises accounting applications as well as cloud-based versions that accept business payments, manage and pay bills, and payroll functions."
---

QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly toward small and medium-sized businesses and offers on-premises accounting applications as well as cloud-based versions that accept business payments, manage and pay bills, and payroll functions.


Categories: accounting


Type: quickbooks/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| base | Base | STRING <details> <summary> Options </summary> https://sandbox-quickbooks.api.intuit.com, https://quickbooks.api.intuit.com </details> | SELECT | The base URL for Quickbooks. | true |
| companyId | Company Id | STRING | TEXT | To get the company id, go to your dashboard. On the top right corner press the gear logo and click Additional information. There you will see your company ID. | true |
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Category
Name: createCategory

Creates a new category.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Name | STRING | TEXT | Name of the category. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| item | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), STRING\(name), STRING\(active), STRING\(fullyQualifiedName), STRING\(type)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Category",
  "name" : "createCategory",
  "parameters" : {
    "name" : ""
  },
  "type" : "quickbooks/v1/createCategory"
}
```


### Create Customer
Name: createCustomer

Creates a new customer.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| displayName | Display Name | STRING | TEXT | The name of the person or organization as displayed. | true |
| givenName | First Name | STRING | TEXT | Given name or first name of a person. | false |
| familyName | Last Name | STRING | TEXT | Family name or the last name of the person. | false |
| suffix | Suffix | STRING | TEXT | Suffix of the name. | false |
| title | Title | STRING | TEXT | Title of the person. | false |
| middleName | Middle Name | STRING | TEXT | Middle name of the person. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| customer | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), STRING\(title), STRING\(givenName), STRING\(middleName), STRING\(familyName), STRING\(suffix), STRING\(fullyQualifiedName), STRING\(displayName), STRING\(active)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Customer",
  "name" : "createCustomer",
  "parameters" : {
    "displayName" : "",
    "givenName" : "",
    "familyName" : "",
    "suffix" : "",
    "title" : "",
    "middleName" : ""
  },
  "type" : "quickbooks/v1/createCustomer"
}
```


### Create Item
Name: createItem

Creates a new item.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Name | STRING | TEXT | Name of the item. | true |
| type | Type | STRING <details> <summary> Options </summary> INVENTORY, SERVICE, NON_INVENTORY </details> | SELECT | Type of item. | true |
| account | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> type </details> | null |  | null |
| expenseAccountRef | Expense Account | STRING | SELECT |  | true |
| qtyOnHand | Quantity on Hand | NUMBER | NUMBER | Current quantity of the inventory items available for sale. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| item | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), STRING\(name), STRING\(active), STRING\(fullyQualifiedName), STRING\(type), {STRING\(name)}\(incomeAccountRef), {STRING\(name)}\(assetAccountRef), {STRING\(name)}\(expenseAccountRef)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Item",
  "name" : "createItem",
  "parameters" : {
    "name" : "",
    "type" : "",
    "account" : { },
    "expenseAccountRef" : "",
    "qtyOnHand" : 0.0
  },
  "type" : "quickbooks/v1/createItem"
}
```


### Create Payment
Name: createPayment

Creates a new payment.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| customer | Customer | STRING | SELECT |  | true |
| totalAmt | Total Amount | NUMBER | NUMBER | Total amount of the transaction. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| payment | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), {STRING\(name)}\(CurrencyRef), {STRING\(name)}\(customerRef), STRING\(totalAmt)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Payment",
  "name" : "createPayment",
  "parameters" : {
    "customer" : "",
    "totalAmt" : 0.0
  },
  "type" : "quickbooks/v1/createPayment"
}
```


### Get Customer
Name: getCustomer

Gets details about a specific customer.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| customer | Customer ID | STRING | SELECT | ID of the customer to get. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| customer | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), STRING\(title), STRING\(givenName), STRING\(middleName), STRING\(familyName), STRING\(suffix), STRING\(fullyQualifiedName), STRING\(displayName), STRING\(active)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Customer",
  "name" : "getCustomer",
  "parameters" : {
    "customer" : ""
  },
  "type" : "quickbooks/v1/getCustomer"
}
```


### Get Invoice
Name: getInvoice

Gets details about a specific invoice.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| invoice | Invoice ID | STRING | SELECT | ID of the invoice to get. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| invoice | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), STRING\(DocNumber), {STRING\(name)}\(customerRef), STRING\(Balance)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Invoice",
  "name" : "getInvoice",
  "parameters" : {
    "invoice" : ""
  },
  "type" : "quickbooks/v1/getInvoice"
}
```


### Get Item
Name: getItem

Gets details about a specific item.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| item | Item ID | STRING | SELECT | ID of the item to get. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| item | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), STRING\(name), STRING\(active), STRING\(fullyQualifiedName), STRING\(type), {STRING\(name)}\(incomeAccountRef), {STRING\(name)}\(assetAccountRef), {STRING\(name)}\(expenseAccountRef)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Item",
  "name" : "getItem",
  "parameters" : {
    "item" : ""
  },
  "type" : "quickbooks/v1/getItem"
}
```


### Get Payment
Name: getPayment

Gets details about a specific payment.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| payment | Payment ID | STRING | SELECT | ID of the payment to get. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| payment | OBJECT <details> <summary> Properties </summary> {STRING\(domain), STRING\(id), {STRING\(name)}\(CurrencyRef), {STRING\(name)}\(customerRef), STRING\(totalAmt)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Payment",
  "name" : "getPayment",
  "parameters" : {
    "payment" : ""
  },
  "type" : "quickbooks/v1/getPayment"
}
```




