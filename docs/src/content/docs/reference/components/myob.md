---
title: "Myob"
description: "MYOB is an accounting software that helps businesses manage their finances, invoicing, and payroll."
---
## Reference
<hr />

MYOB is an accounting software that helps businesses manage their finances, invoicing, and payroll.


Categories: [accounting]


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
| API key | STRING | TEXT  |  The API key registered in https://my.myob.com.au/au/bd/DevAppList.aspx  |





<hr />





## Actions


### Create Customer
Creates a new customer.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company File | STRING | SELECT  |  The MYOB company file to use.  |
| Is Individual? | BOOLEAN | SELECT  |  Does customer contact represent an individual or a company?  |
| First Name | STRING | TEXT  |  First name for an individual contact.  |
| Last Name | STRING | TEXT  |  Last name for an individual contact.  |
| Company Name | STRING | TEXT  |  Company name of the customer contact.  |
| Is Active? | BOOLEAN | SELECT  |  Is customer contact active?  |
| Addresses | [{STRING\(Street), STRING\(City), STRING\(State), STRING\(PostCode), STRING\(Country), STRING\(Phone1), STRING\(Email), STRING\(Website)}] | ARRAY_BUILDER  |  List of addresses for the customer contact.  |




### Create Customer Payment
Creates a new customer payment.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company File | STRING | SELECT  |  The MYOB company file to use.  |
| Pay From | STRING | SELECT  |  |
| Account | STRING | TEXT  |  |
| Customer | STRING | SELECT  |  |




### Create Supplier
Creates a new supplier.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company File | STRING | SELECT  |  The MYOB company file to use.  |
| Is Individual? | BOOLEAN | SELECT  |  Does supplier contact represent an individual or a company?  |
| First Name | STRING | TEXT  |  First name for an individual contact.  |
| Last Name | STRING | TEXT  |  Last name for an individual contact.  |
| Company Name | STRING | TEXT  |  Company name of the supplier contact.  |
| Is Active? | BOOLEAN | SELECT  |  Is supplier contact active?  |
| Addresses | [{STRING\(Street), STRING\(City), STRING\(State), STRING\(PostCode), STRING\(Country), STRING\(Phone1), STRING\(Email), STRING\(Website)}] | ARRAY_BUILDER  |  List of addresses for the customer contact.  |




### Create Supplier Payment
Creates a new supplier payment.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Company File | STRING | SELECT  |  The MYOB company file to use.  |
| Pay From | STRING | SELECT  |  |
| Account | STRING | TEXT  |  |
| Supplier | STRING | SELECT  |  |




