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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Company Id | STRING | TEXT  |  To get the company id, go to your dashboard. On the top right corner press the gear logo and click Additional information. There you will see your company ID  |





<hr />





## Actions


### Create customer
Has conditionally required parameters.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Display name | STRING | TEXT  |  The name of the person or organization as displayed. Must be unique across all Customer, Vendor, and Employee objects. Cannot be removed with sparse update. If not supplied, the system generates DisplayName by concatenating customer name components supplied in the request from the following list: Title, GivenName, MiddleName, FamilyName, and Suffix.  |
| Suffix | STRING | TEXT  |  Suffix of the name. For example, Jr. The DisplayName attribute or at least one of Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.  |
| Title | STRING | TEXT  |  Title of the person. This tag supports i18n, all locales. The DisplayName attribute or at least one of Title, GivenName, MiddleName, FamilyName, Suffix, or FullyQualifiedName attributes are required during create.  |
| Middle name | STRING | TEXT  |  Middle name of the person. The person can have zero or more middle names. The DisplayName attribute or at least one of Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.  |
| Last/Family name | STRING | TEXT  |  Family name or the last name of the person. The DisplayName attribute or at least one of Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.  |
| First/Given name | STRING | TEXT  |  Given name or first name of a person. The DisplayName attribute or at least one of Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ID | STRING | TEXT  |  |
| Contact name | STRING | TEXT  |  |
| Credit card | {STRING(number), STRING(nameOnAcct), INTEGER(ccExpiryMonth), INTEGER(ccExpiryYear), STRING(billAddrStreet), STRING(postalCode), NUMBER(amount)} | OBJECT_BUILDER  |  |
| Balance | NUMBER | NUMBER  |  |
| Account number | STRING | TEXT  |  |
| Business number | STRING | TEXT  |  |





### Create item
Creates a new item.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Name of the item. This value must be unique. Required for create.  |
| Quantity on hand | NUMBER | NUMBER  |  Current quantity of the Inventory items available for sale. Not used for Service or NonInventory type items.Required for Inventory type items.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| ID | STRING | TEXT  |  |
| Name | STRING | TEXT  |  |
| Description | STRING | TEXT  |  |
| Unit price | NUMBER | NUMBER  |  |





### Create a category
Has conditionally required parameters.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Nane | STRING | TEXT  |  Name of the category  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(id), STRING(domain), STRING(Name), STRING(Level), STRING(Subitem), STRING(FullyQualifiedName)} | OBJECT_BUILDER  |





