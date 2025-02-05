---
title: "Myob"
description: "MYOB is an accounting software that helps businesses manage their finances, invoicing, and payroll."
---

MYOB is an accounting software that helps businesses manage their finances, invoicing, and payroll.


Categories: accounting


Type: myob/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| key | API key | STRING | TEXT  |  The API key registered in https://my.myob.com.au/au/bd/DevAppList.aspx  |  true  |





<hr />



## Actions


### Create Customer
Creates a new customer.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| companyFile | Company File | STRING | SELECT  |  The MYOB company file to use.  |  true  |
| IsIndividual | Is Individual? | BOOLEAN | SELECT  |  Does customer contact represent an individual or a company?  |  true  |
| FirstName | First Name | STRING | TEXT  |  First name for an individual contact.  |  true  |
| LastName | Last Name | STRING | TEXT  |  Last name for an individual contact.  |  true  |
| CompanyName | Company Name | STRING | TEXT  |  Company name of the customer contact.  |  true  |
| IsActive | Is Active? | BOOLEAN | SELECT  |  Is customer contact active?  |  true  |
| Addresses | Addresses | [{STRING\(Street), STRING\(City), STRING\(State), STRING\(PostCode), STRING\(Country), STRING\(Phone1), STRING\(Email), STRING\(Website)}] | ARRAY_BUILDER  |  List of addresses for the customer contact.  |  false  |




### Create Customer Payment
Creates a new customer payment.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| companyFile | Company File | STRING | SELECT  |  The MYOB company file to use.  |  true  |
| PayFrom | Pay From | STRING | SELECT  |  | true  |
| Account | Account | STRING | TEXT  |  | true  |
| Customer | Customer UID | STRING | SELECT  |  | true  |




### Create Supplier
Creates a new supplier.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| companyFile | Company File | STRING | SELECT  |  The MYOB company file to use.  |  true  |
| IsIndividual | Is Individual? | BOOLEAN | SELECT  |  Does supplier contact represent an individual or a company?  |  true  |
| FirstName | First Name | STRING | TEXT  |  First name for an individual contact.  |  true  |
| LastName | Last Name | STRING | TEXT  |  Last name for an individual contact.  |  true  |
| CompanyName | Company Name | STRING | TEXT  |  Company name of the supplier contact.  |  true  |
| IsActive | Is Active? | BOOLEAN | SELECT  |  Is supplier contact active?  |  false  |
| Addresses | Addresses | [{STRING\(Street), STRING\(City), STRING\(State), STRING\(PostCode), STRING\(Country), STRING\(Phone1), STRING\(Email), STRING\(Website)}] | ARRAY_BUILDER  |  List of addresses for the customer contact.  |  false  |




### Create Supplier Payment
Creates a new supplier payment.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| companyFile | Company File | STRING | SELECT  |  The MYOB company file to use.  |  true  |
| PayFrom | Pay From | STRING | SELECT  |  | true  |
| Account | Account | STRING | TEXT  |  | true  |
| Supplier | Supplier UID | STRING | SELECT  |  | true  |






