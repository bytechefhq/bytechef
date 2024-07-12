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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |





<hr />





## Actions


### Create bill
Creates draft bill (Accounts Payable).

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Contact | STRING | SELECT  |
| Date | DATE | DATE  |
| Due Date | DATE | DATE  |
| Line amount type | STRING | SELECT  |
| Line items | ARRAY | ARRAY_BUILDER  |
| Currency | STRING | SELECT  |
| Invoice Reference | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |





### Create contact
Creates a new contact.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Name | STRING | TEXT  |
| Company Number | STRING | TEXT  |
| Account number | STRING | TEXT  |
| Contact status | STRING | SELECT  |
| First name | STRING | TEXT  |
| Last name | STRING | TEXT  |
| Email address | STRING | EMAIL  |
| Bank account number | STRING | TEXT  |
| Tax number | STRING | TEXT  |
| Phones | ARRAY | ARRAY_BUILDER  |
| Addresses | ARRAY | ARRAY_BUILDER  |


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
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |





### Create invoice
Creates draft invoice (Acount Receivable).

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Contact | STRING | SELECT  |
| Date | DATE | DATE  |
| Due Date | DATE | DATE  |
| Line amount type | STRING | SELECT  |
| Line items | ARRAY | ARRAY_BUILDER  |
| Currency | STRING | SELECT  |
| Invoice Reference | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |





### Create quote
Creates a new quote draft.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Contact | STRING | SELECT  |
| Date | DATE | DATE  |
| Line items | ARRAY | ARRAY_BUILDER  |
| Line amount type | STRING | SELECT  |
| Expiry date | DATE | DATE  |
| Currency | STRING | SELECT  |
| Quote number | STRING | TEXT  |
| Reference | STRING | TEXT  |
| Branding theme | STRING | SELECT  |
| Title | STRING | TEXT  |
| Summary | STRING | TEXT  |
| Terms | STRING | TEXT_AREA  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





