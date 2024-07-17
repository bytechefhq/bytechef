---
title: "Xero"
description: "Xero is an online accounting software platform designed for small businesses and accountants to manage finances efficiently."
---
## Reference
<hr />

Xero is an online accounting software platform designed for small businesses and accountants to manage finances efficiently.


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


### New Bill
Trigger off whenever a new bill is added.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Webhook key | STRING | TEXT  |  The key used to sign the webhook request.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(ContactID), STRING(Name), STRING(EmailAddress)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(Description), INTEGER(Quantity), NUMBER(UnitAmount)}] | ARRAY_BUILDER  |
| STRING | TEXT  |






### New Contact
Triggers when a contact is created.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Webhook key | STRING | TEXT  |  The key used to sign the webhook request.  |


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
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(AddressType), STRING(City), STRING(Region), STRING(PostalCode), STRING(Country)}] | ARRAY_BUILDER  |
| [{STRING(PhoneType), STRING(PhoneNumber), STRING(PhoneAreaCode), STRING(PhoneCountryCode)}] | ARRAY_BUILDER  |






### New Invoice
Trigger off whenever a new invoice is added.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Webhook key | STRING | TEXT  |  The key used to sign the webhook request.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(ContactID), STRING(Name), STRING(EmailAddress)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(Description), INTEGER(Quantity), NUMBER(UnitAmount)}] | ARRAY_BUILDER  |
| STRING | TEXT  |






<hr />



## Actions


### Create bill
Creates draft bill (Accounts Payable).

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | STRING | SELECT  |  Contact to create the bill for.  |
| Date | DATE | DATE  |  Date of the bill. If no date is specified, the current date will be used.  |
| Due Date | DATE | DATE  |  Date bill is due.If no date is specified, the current date will be used.   |
| Line amount type | STRING | SELECT  |  |
| Line items | [{STRING(Description), NUMBER(Quantity), NUMBER(UnitAmount), STRING(AccountCode)}($LineItem)] | ARRAY_BUILDER  |  Line items on the bill.  |
| Currency | STRING | SELECT  |  Currency that bill is raised in.  |
| Invoice Reference | STRING | TEXT  |  Reference number of the bill.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(ContactID), STRING(Name), STRING(EmailAddress)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(Description), INTEGER(Quantity), NUMBER(UnitAmount)}] | ARRAY_BUILDER  |
| STRING | TEXT  |





### Create contact
Creates a new contact.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Full name of a contact or organisation.  |
| Company Number | STRING | TEXT  |  Company registration number.  |
| Account number | STRING | TEXT  |  Unique account number to identify, reference and search for the contact.  |
| Contact status | STRING | SELECT  |  Current status of a contact.  |
| First name | STRING | TEXT  |  First name of primary person.  |
| Last name | STRING | TEXT  |  Last name of primary person.  |
| Email address | STRING | EMAIL  |  Email address of contact person.  |
| Bank account number | STRING | TEXT  |  Bank account number of contact.  |
| Tax number | STRING | TEXT  |  Tax number of contact – this is also known as the ABN (Australia), GST Number (New Zealand), VAT Number (UK) or Tax ID Number (US and global) in the Xero UI depending on which regionalized version of Xero you are using.  |
| Phones | [{STRING(PhoneType), STRING(PhoneNumber), STRING(PhoneAreaCode), STRING(PhoneCountryCode)}] | ARRAY_BUILDER  |  |
| Addresses | [{STRING(AddressType), STRING(City), STRING(Region), STRING(PostalCode), STRING(Country)}] | ARRAY_BUILDER  |  |


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
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(AddressType), STRING(City), STRING(Region), STRING(PostalCode), STRING(Country)}] | ARRAY_BUILDER  |
| [{STRING(PhoneType), STRING(PhoneNumber), STRING(PhoneAreaCode), STRING(PhoneCountryCode)}] | ARRAY_BUILDER  |





### Create invoice
Creates draft invoice (Acount Receivable).

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | STRING | SELECT  |  Contact to create the invoice for.  |
| Date | DATE | DATE  |  Date invoice was issued. If no date is specified, the current date will be used.  |
| Due Date | DATE | DATE  |  Date invoice is due. If no date is specified, the current date will be used.  |
| Line amount type | STRING | SELECT  |  |
| Line items | [{STRING(Description), INTEGER(Quantity), NUMBER(UnitAmount), NUMBER(DiscountRate)}] | ARRAY_BUILDER  |  Line items on the invoice.  |
| Currency | STRING | SELECT  |  Currency that invoice is raised in.  |
| Invoice Reference | STRING | TEXT  |  Reference number of the invoice.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(ContactID), STRING(Name), STRING(EmailAddress)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(Description), INTEGER(Quantity), NUMBER(UnitAmount)}] | ARRAY_BUILDER  |
| STRING | TEXT  |





### Create quote
Creates a new quote draft.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Contact | STRING | SELECT  |  Full name of a contact or organisation.  |
| Date | DATE | DATE  |  Date quote was issued.  |
| Line items | [{STRING(Description), INTEGER(Quantity), NUMBER(UnitAmount), NUMBER(DiscountRate)}] | ARRAY_BUILDER  |  Line items on the invoice.  |
| Line amount type | STRING | SELECT  |  |
| Expiry date | DATE | DATE  |  Date quote expires  |
| Currency | STRING | SELECT  |  The currency that quote has been raised in.  |
| Quote number | STRING | TEXT  |  Unique alpha numeric code identifying a quote.  |
| Reference | STRING | TEXT  |  Additional reference number  |
| Branding theme | STRING | SELECT  |  The branding theme to be applied to this quote.  |
| Title | STRING | TEXT  |  The title of the quote.  |
| Summary | STRING | TEXT  |  The summary of the quote.  |
| Terms | STRING | TEXT_AREA  |  The terms of the quote.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(ContactID), STRING(Name), STRING(EmailAddress)} | OBJECT_BUILDER  |
| [{STRING(LineItemID), STRING(Description), NUMBER(UnitAmount), INTEGER(DiscountRate), INTEGER(Quantity)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





