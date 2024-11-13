---
title: "QuickBooks"
description: "QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly toward small and medium-sized businesses and offers on-premises accounting applications as well as cloud-based versions that accept business payments, manage and pay bills, and payroll functions."
---
## Reference
<hr />

QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly toward small and medium-sized businesses and offers on-premises accounting applications as well as cloud-based versions that accept business payments, manage and pay bills, and payroll functions.


Categories: [accounting]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Base | STRING | SELECT  |  The base URL for Quickbooks.  |
| Company Id | STRING | TEXT  |  To get the company id, go to your dashboard. On the top right corner press the gear logo and click Additional information. There you will see your company ID.  |
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />





## Actions


### Create Category
Creates a new category.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Name of the category.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), STRING\(Name), STRING\(Active), STRING\(FullyQualifiedName), STRING\(Type)} | OBJECT_BUILDER  |






### Create Customer
Creates a new customer.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Display Name | STRING | TEXT  |  The name of the person or organization as displayed.  |
| First Name | STRING | TEXT  |  Given name or first name of a person.  |
| Last Name | STRING | TEXT  |  Family name or the last name of the person.  |
| Suffix | STRING | TEXT  |  Suffix of the name.  |
| Title | STRING | TEXT  |  Title of the person.  |
| Middle Name | STRING | TEXT  |  Middle name of the person.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), STRING\(Title), STRING\(GivenName), STRING\(MiddleName), STRING\(FamilyName), STRING\(Suffix), STRING\(FullyQualifiedName), STRING\(DisplayName), STRING\(Active)} | OBJECT_BUILDER  |






### Create Item
Creates a new item.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Name of the item.  |
| Type | STRING | SELECT  |  Type of item.  |
| DYNAMIC_PROPERTIES | null  |
| Expense Account | STRING | SELECT  |  |
| Quantity on Hand | NUMBER | NUMBER  |  Current quantity of the inventory items available for sale.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), STRING\(Name), STRING\(Active), STRING\(FullyQualifiedName), STRING\(Type), {STRING\(name)}\(IncomeAccountRef), {STRING\(name)}\(AssetAccountRef), {STRING\(name)}\(ExpenseAccountRef)} | OBJECT_BUILDER  |






### Create Payment
Creates a new payment.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Customer | STRING | SELECT  |  |
| Total Amount | NUMBER | NUMBER  |  Total amount of the transaction.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), {STRING\(name)}\(CurrencyRef), {STRING\(name)}\(CustomerRef), STRING\(TotalAmt)} | OBJECT_BUILDER  |






### Get Customer
Gets details about a specific customer.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Customer | STRING | SELECT  |  Customer to get.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), STRING\(Title), STRING\(GivenName), STRING\(MiddleName), STRING\(FamilyName), STRING\(Suffix), STRING\(FullyQualifiedName), STRING\(DisplayName), STRING\(Active)} | OBJECT_BUILDER  |






### Get Invoice
Gets details about a specific invoice.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Invoice | STRING | SELECT  |  Invoice to get.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), STRING\(DocNumber), {STRING\(name)}\(CustomerRef), STRING\(Balance)} | OBJECT_BUILDER  |






### Get Item
Gets details about a specific item.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Item | STRING | SELECT  |  Item to get.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), STRING\(Name), STRING\(Active), STRING\(FullyQualifiedName), STRING\(Type), {STRING\(name)}\(IncomeAccountRef), {STRING\(name)}\(AssetAccountRef), {STRING\(name)}\(ExpenseAccountRef)} | OBJECT_BUILDER  |






### Get Payment
Gets details about a specific payment.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Payment | STRING | SELECT  |  Payment to get.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(domain), STRING\(Id), {STRING\(name)}\(CurrencyRef), {STRING\(name)}\(CustomerRef), STRING\(TotalAmt)} | OBJECT_BUILDER  |






