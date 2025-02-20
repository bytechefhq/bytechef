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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| key | API key | STRING | TEXT | The API key registered in https://my.myob.com.au/au/bd/DevAppList.aspx | true |





<hr />



## Actions


### Create Customer
Name: createCustomer

Creates a new customer.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| companyFile | Company File | STRING | SELECT | The MYOB company file to use. | true |
| IsIndividual | Is Individual? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Does customer contact represent an individual or a company? | true |
| FirstName | First Name | STRING | TEXT | First name for an individual contact. | true |
| LastName | Last Name | STRING | TEXT | Last name for an individual contact. | true |
| CompanyName | Company Name | STRING | TEXT | Company name of the customer contact. | true |
| IsActive | Is Active? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Is customer contact active? | true |
| Addresses | Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(Street), STRING\(City), STRING\(State), STRING\(PostCode), STRING\(Country), STRING\(Phone1), STRING\(Email), STRING\(Website)}] </details> | ARRAY_BUILDER | List of addresses for the customer contact. | false |


#### JSON Example
```json
{
  "label" : "Create Customer",
  "name" : "createCustomer",
  "parameters" : {
    "companyFile" : "",
    "IsIndividual" : false,
    "FirstName" : "",
    "LastName" : "",
    "CompanyName" : "",
    "IsActive" : false,
    "Addresses" : [ {
      "Street" : "",
      "City" : "",
      "State" : "",
      "PostCode" : "",
      "Country" : "",
      "Phone1" : "",
      "Email" : "",
      "Website" : ""
    } ]
  },
  "type" : "myob/v1/createCustomer"
}
```


### Create Customer Payment
Name: createCustomerPayment

Creates a new customer payment.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| companyFile | Company File | STRING | SELECT | The MYOB company file to use. | true |
| PayFrom | Pay From | STRING <details> <summary> Options </summary> Account, ElectronicPayments </details> | SELECT |  | true |
| Account | Account | STRING | TEXT |  | true |
| Customer | Customer UID | STRING <details> <summary> Depends On </summary> companyFile </details> | SELECT |  | true |


#### JSON Example
```json
{
  "label" : "Create Customer Payment",
  "name" : "createCustomerPayment",
  "parameters" : {
    "companyFile" : "",
    "PayFrom" : "",
    "Account" : "",
    "Customer" : ""
  },
  "type" : "myob/v1/createCustomerPayment"
}
```


### Create Supplier
Name: createSupplier

Creates a new supplier.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| companyFile | Company File | STRING | SELECT | The MYOB company file to use. | true |
| IsIndividual | Is Individual? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Does supplier contact represent an individual or a company? | true |
| FirstName | First Name | STRING | TEXT | First name for an individual contact. | true |
| LastName | Last Name | STRING | TEXT | Last name for an individual contact. | true |
| CompanyName | Company Name | STRING | TEXT | Company name of the supplier contact. | true |
| IsActive | Is Active? | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Is supplier contact active? | false |
| Addresses | Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(Street), STRING\(City), STRING\(State), STRING\(PostCode), STRING\(Country), STRING\(Phone1), STRING\(Email), STRING\(Website)}] </details> | ARRAY_BUILDER | List of addresses for the customer contact. | false |


#### JSON Example
```json
{
  "label" : "Create Supplier",
  "name" : "createSupplier",
  "parameters" : {
    "companyFile" : "",
    "IsIndividual" : false,
    "FirstName" : "",
    "LastName" : "",
    "CompanyName" : "",
    "IsActive" : false,
    "Addresses" : [ {
      "Street" : "",
      "City" : "",
      "State" : "",
      "PostCode" : "",
      "Country" : "",
      "Phone1" : "",
      "Email" : "",
      "Website" : ""
    } ]
  },
  "type" : "myob/v1/createSupplier"
}
```


### Create Supplier Payment
Name: createSupplierPayment

Creates a new supplier payment.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| companyFile | Company File | STRING | SELECT | The MYOB company file to use. | true |
| PayFrom | Pay From | STRING <details> <summary> Options </summary> Account, ElectronicPayments </details> | SELECT |  | true |
| Account | Account | STRING | TEXT |  | true |
| Supplier | Supplier UID | STRING <details> <summary> Depends On </summary> companyFile </details> | SELECT |  | true |


#### JSON Example
```json
{
  "label" : "Create Supplier Payment",
  "name" : "createSupplierPayment",
  "parameters" : {
    "companyFile" : "",
    "PayFrom" : "",
    "Account" : "",
    "Supplier" : ""
  },
  "type" : "myob/v1/createSupplierPayment"
}
```




