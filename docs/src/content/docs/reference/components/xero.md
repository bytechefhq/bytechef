---
title: "Xero"
description: "Xero is an online accounting software platform designed for small businesses and accountants to manage finances efficiently."
---

Xero is an online accounting software platform designed for small businesses and accountants to manage finances efficiently.


Categories: accounting


Type: xero/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Bill
Name: createBill

Creates draft bill (Accounts Payable).

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| ContactID | Contact ID | STRING | SELECT | ID of the contact to create the bill for. | true |
| Date | Date | DATE | DATE | Date of the bill. If no date is specified, the current date will be used. | true |
| DueDate | Due Date | DATE | DATE | Date bill is due. If no date is specified, the current date will be used. | false |
| LineAmountTypes | Line Amount Type | STRING <details> <summary> Options </summary> Exclusive, Inclusive, NoTax </details> | SELECT |  | false |
| LineItems | Line Items | ARRAY <details> <summary> Items </summary> [{STRING\(Description), NUMBER\(Quantity), NUMBER\(UnitAmount), STRING\(AccountCode)}\($LineItem)] </details> | ARRAY_BUILDER | Line items on the bill. | true |
| CurrencyCode | Currency | STRING | SELECT | Currency that bill is raised in. | false |
| Reference | Invoice Reference | STRING | TEXT | Reference number of the bill. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| Type | STRING | TEXT |
| Reference | STRING | TEXT |
| Contact | OBJECT <details> <summary> Properties </summary> {STRING\(ContactID), STRING\(Name), STRING\(EmailAddress)} </details> | OBJECT_BUILDER |
| DateString | STRING | TEXT |
| DueDateString | STRING | TEXT |
| Status | STRING | TEXT |
| LineAmountTypes | STRING | TEXT |
| LineItems | ARRAY <details> <summary> Items </summary> [{STRING\(Description), INTEGER\(Quantity), NUMBER\(UnitAmount)}] </details> | ARRAY_BUILDER |
| CurrencyCode | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Bill",
  "name" : "createBill",
  "parameters" : {
    "ContactID" : "",
    "Date" : "2021-01-01",
    "DueDate" : "2021-01-01",
    "LineAmountTypes" : "",
    "LineItems" : [ {
      "Description" : "",
      "Quantity" : 0.0,
      "UnitAmount" : 0.0,
      "AccountCode" : ""
    } ],
    "CurrencyCode" : "",
    "Reference" : ""
  },
  "type" : "xero/v1/createBill"
}
```


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| Name | Name | STRING | TEXT | Full name of a contact or organisation. | true |
| CompanyNumber | Company Number | STRING | TEXT | Company registration number. | false |
| AccountNumber | Account Number | STRING | TEXT | Unique account number to identify, reference and search for the contact. | false |
| ContactStatus | Contact Status | STRING <details> <summary> Options </summary> ACTIVE, ARCHIVED, GDPRREQUEST </details> | SELECT | Current status of a contact. | false |
| FirstName | First Name | STRING | TEXT | First name of primary person. | false |
| LastName | Last Name | STRING | TEXT | Last name of primary person. | false |
| EmailAddress | Email Address | STRING | EMAIL | Email address of contact person. | false |
| BankAccountDetails | Bank Account Number | STRING | TEXT | Bank account number of contact. | false |
| TaxNumber | Tax Number | STRING | TEXT | Tax number of contact – this is also known as the ABN (Australia), GST Number (New Zealand), VAT Number (UK) or Tax ID Number (US and global) in the Xero UI depending on which regionalized version of Xero you are using. | false |
| Phones | Phones | ARRAY <details> <summary> Items </summary> [{STRING\(PhoneType), STRING\(PhoneNumber), STRING\(PhoneAreaCode), STRING\(PhoneCountryCode)}] </details> | ARRAY_BUILDER |  | false |
| Addresses | Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(AddressType), STRING\(City), STRING\(Region), STRING\(PostalCode), STRING\(Country)}] </details> | ARRAY_BUILDER |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| ContactID | STRING | TEXT |
| CompanyNumber | STRING | TEXT |
| AccountNumber | STRING | TEXT |
| ContactStatus | STRING | TEXT |
| Name | STRING | TEXT |
| FirstName | STRING | TEXT |
| LastName | STRING | TEXT |
| EmailAddress | STRING | TEXT |
| BankAccountDetails | STRING | TEXT |
| TaxNumber | STRING | TEXT |
| Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(AddressType), STRING\(City), STRING\(Region), STRING\(PostalCode), STRING\(Country)}] </details> | ARRAY_BUILDER |
| Phones | ARRAY <details> <summary> Items </summary> [{STRING\(PhoneType), STRING\(PhoneNumber), STRING\(PhoneAreaCode), STRING\(PhoneCountryCode)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "Name" : "",
    "CompanyNumber" : "",
    "AccountNumber" : "",
    "ContactStatus" : "",
    "FirstName" : "",
    "LastName" : "",
    "EmailAddress" : "",
    "BankAccountDetails" : "",
    "TaxNumber" : "",
    "Phones" : [ {
      "PhoneType" : "",
      "PhoneNumber" : "",
      "PhoneAreaCode" : "",
      "PhoneCountryCode" : ""
    } ],
    "Addresses" : [ {
      "AddressType" : "",
      "City" : "",
      "Region" : "",
      "PostalCode" : "",
      "Country" : ""
    } ]
  },
  "type" : "xero/v1/createContact"
}
```


### Create Invoice
Name: createSalesInvoice

Creates draft invoice (Acount Receivable).

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| ContactID | Contact ID | STRING | SELECT | ID of the contact to create the invoice for. | true |
| Date | Date | DATE | DATE | Date invoice was issued. If no date is specified, the current date will be used. | false |
| DueDate | Due Date | DATE | DATE | Date invoice is due. If no date is specified, the current date will be used. | false |
| LineAmountTypes | Line Amount Type | STRING <details> <summary> Options </summary> Exclusive, Inclusive, NoTax </details> | SELECT |  | false |
| LineItems | Line Items | ARRAY <details> <summary> Items </summary> [{STRING\(Description), INTEGER\(Quantity), NUMBER\(UnitAmount), NUMBER\(DiscountRate)}] </details> | ARRAY_BUILDER | Line items on the invoice. | true |
| CurrencyCode | Currency Code | STRING | SELECT | Currency code that invoice is raised in. | false |
| Reference | Invoice Reference | STRING | TEXT | Reference number of the invoice. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| Type | STRING | TEXT |
| Reference | STRING | TEXT |
| Contact | OBJECT <details> <summary> Properties </summary> {STRING\(ContactID), STRING\(Name), STRING\(EmailAddress)} </details> | OBJECT_BUILDER |
| DateString | STRING | TEXT |
| DueDateString | STRING | TEXT |
| Status | STRING | TEXT |
| LineAmountTypes | STRING | TEXT |
| LineItems | ARRAY <details> <summary> Items </summary> [{STRING\(Description), INTEGER\(Quantity), NUMBER\(UnitAmount)}] </details> | ARRAY_BUILDER |
| CurrencyCode | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Invoice",
  "name" : "createSalesInvoice",
  "parameters" : {
    "ContactID" : "",
    "Date" : "2021-01-01",
    "DueDate" : "2021-01-01",
    "LineAmountTypes" : "",
    "LineItems" : [ {
      "Description" : "",
      "Quantity" : 1,
      "UnitAmount" : 0.0,
      "DiscountRate" : 0.0
    } ],
    "CurrencyCode" : "",
    "Reference" : ""
  },
  "type" : "xero/v1/createSalesInvoice"
}
```


### Create Quote
Name: createQuote

Creates a new quote draft.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| ContactID | Contact ID | STRING | SELECT | ID of the contact that the quote is being raised for. | true |
| Date | Date | DATE | DATE | Date quote was issued. | true |
| LineItems | Line Items | ARRAY <details> <summary> Items </summary> [{STRING\(Description), INTEGER\(Quantity), NUMBER\(UnitAmount), NUMBER\(DiscountRate)}] </details> | ARRAY_BUILDER | Line items on the invoice. | true |
| LineAmountTypes | Line Amount Type | STRING <details> <summary> Options </summary> Exclusive, Inclusive, NoTax </details> | SELECT |  | false |
| ExpiryDate | Expiry Date | DATE | DATE | Date quote expires | false |
| CurrencyCode | Currency Code | STRING | SELECT | The currency code that quote has been raised in. | false |
| QuoteNumber | Quote Number | STRING | TEXT | Unique alpha numeric code identifying a quote. | false |
| Reference | Reference | STRING | TEXT | Additional reference number | false |
| BrandingThemeID | Branding Theme ID | STRING | SELECT | The branding theme ID to be applied to this quote. | false |
| Title | Title | STRING | TEXT | The title of the quote. | false |
| Summary | Summary | STRING | TEXT | The summary of the quote. | false |
| Terms | Terms | STRING | TEXT_AREA | The terms of the quote. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| QuoteID | STRING | TEXT |
| QuoteNumber | STRING | TEXT |
| Reference | STRING | TEXT |
| Terms | STRING | TEXT |
| Contact | OBJECT <details> <summary> Properties </summary> {STRING\(ContactID), STRING\(Name), STRING\(EmailAddress)} </details> | OBJECT_BUILDER |
| LineItems | ARRAY <details> <summary> Items </summary> [{STRING\(LineItemID), STRING\(Description), NUMBER\(UnitAmount), INTEGER\(DiscountRate), INTEGER\(Quantity)}] </details> | ARRAY_BUILDER |
| DateString | STRING | TEXT |
| ExpiryDateString | STRING | TEXT |
| Status | STRING | TEXT |
| CurrencyCode | STRING | TEXT |
| Title | STRING | TEXT |
| BrandingThemeID | STRING | TEXT |
| Summary | STRING | TEXT |
| LineAmountTypes | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Quote",
  "name" : "createQuote",
  "parameters" : {
    "ContactID" : "",
    "Date" : "2021-01-01",
    "LineItems" : [ {
      "Description" : "",
      "Quantity" : 1,
      "UnitAmount" : 0.0,
      "DiscountRate" : 0.0
    } ],
    "LineAmountTypes" : "",
    "ExpiryDate" : "2021-01-01",
    "CurrencyCode" : "",
    "QuoteNumber" : "",
    "Reference" : "",
    "BrandingThemeID" : "",
    "Title" : "",
    "Summary" : "",
    "Terms" : ""
  },
  "type" : "xero/v1/createQuote"
}
```




## Triggers


### New Bill
Name: newBill

Trigger off whenever a new bill is added.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| webhookKey | Webhook Key | STRING | TEXT | The key used to sign the webhook request. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| Type | STRING | TEXT |
| Reference | STRING | TEXT |
| Contact | OBJECT <details> <summary> Properties </summary> {STRING\(ContactID), STRING\(Name), STRING\(EmailAddress)} </details> | OBJECT_BUILDER |
| DateString | STRING | TEXT |
| DueDateString | STRING | TEXT |
| Status | STRING | TEXT |
| LineAmountTypes | STRING | TEXT |
| LineItems | ARRAY <details> <summary> Items </summary> [{STRING\(Description), INTEGER\(Quantity), NUMBER\(UnitAmount)}] </details> | ARRAY_BUILDER |
| CurrencyCode | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Bill",
  "name" : "newBill",
  "parameters" : {
    "webhookKey" : ""
  },
  "type" : "xero/v1/newBill"
}
```


### New Contact
Name: newContact

Triggers when a contact is created.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| webhookKey | Webhook Key | STRING | TEXT | The key used to sign the webhook request. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| ContactID | STRING | TEXT |
| CompanyNumber | STRING | TEXT |
| AccountNumber | STRING | TEXT |
| ContactStatus | STRING | TEXT |
| Name | STRING | TEXT |
| FirstName | STRING | TEXT |
| LastName | STRING | TEXT |
| EmailAddress | STRING | TEXT |
| BankAccountDetails | STRING | TEXT |
| TaxNumber | STRING | TEXT |
| Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(AddressType), STRING\(City), STRING\(Region), STRING\(PostalCode), STRING\(Country)}] </details> | ARRAY_BUILDER |
| Phones | ARRAY <details> <summary> Items </summary> [{STRING\(PhoneType), STRING\(PhoneNumber), STRING\(PhoneAreaCode), STRING\(PhoneCountryCode)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "New Contact",
  "name" : "newContact",
  "parameters" : {
    "webhookKey" : ""
  },
  "type" : "xero/v1/newContact"
}
```


### New Invoice
Name: newInvoice

Trigger off whenever a new invoice is added.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| webhookKey | Webhook Key | STRING | TEXT | The key used to sign the webhook request. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| Type | STRING | TEXT |
| Reference | STRING | TEXT |
| Contact | OBJECT <details> <summary> Properties </summary> {STRING\(ContactID), STRING\(Name), STRING\(EmailAddress)} </details> | OBJECT_BUILDER |
| DateString | STRING | TEXT |
| DueDateString | STRING | TEXT |
| Status | STRING | TEXT |
| LineAmountTypes | STRING | TEXT |
| LineItems | ARRAY <details> <summary> Items </summary> [{STRING\(Description), INTEGER\(Quantity), NUMBER\(UnitAmount)}] </details> | ARRAY_BUILDER |
| CurrencyCode | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Invoice",
  "name" : "newInvoice",
  "parameters" : {
    "webhookKey" : ""
  },
  "type" : "xero/v1/newInvoice"
}
```


<hr />

