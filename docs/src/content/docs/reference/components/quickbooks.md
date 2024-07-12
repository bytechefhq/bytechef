---
title: "QuickBooks"
description: "QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly toward small and medium-sized businesses and offers on-premises accounting applications as well as cloud-based versions that accept business payments, manage and pay bills, and payroll functions."
---
## Reference
<hr />

QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly toward small and medium-sized businesses and offers on-premises accounting applications as well as cloud-based versions that accept business payments, manage and pay bills, and payroll functions.


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
| Company Id | STRING | TEXT  |





<hr />





## Actions


### Create customer
Has conditionally required parameters.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Display name | STRING | TEXT  |
| Suffix | STRING | TEXT  |
| Title | STRING | TEXT  |
| Middle name | STRING | TEXT  |
| Last/Family name | STRING | TEXT  |
| First/Given name | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ID | STRING | TEXT  |
| Contact name | STRING | TEXT  |
| Credit card | OBJECT | OBJECT_BUILDER  |
| Balance | NUMBER | NUMBER  |
| Account number | STRING | TEXT  |
| Business number | STRING | TEXT  |





### Create item
Creates a new item.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Name | STRING | TEXT  |
| Quantity on hand | NUMBER | NUMBER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ID | STRING | TEXT  |
| Name | STRING | TEXT  |
| Description | STRING | TEXT  |
| Unit price | NUMBER | NUMBER  |





### Create a category
Has conditionally required parameters.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Nane | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |





