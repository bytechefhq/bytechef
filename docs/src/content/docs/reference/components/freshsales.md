---
title: "Freshsales"
description: "Freshsales is a customer relationship management (CRM) software designed to help businesses streamline sales processes and manage customer interactions effectively."
---

Freshsales is a customer relationship management (CRM) software designed to help businesses streamline sales processes and manage customer interactions effectively.


Categories: crm


Type: freshsales/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Bundle alias | STRING | TEXT  |  Your Freshsales bundle alias (e.g. https://<alias>.myfreshworks.com)  |  true  |
| key | API Key | STRING | TEXT  |  The API Key supplied by Freshsales  |  true  |





<hr />



## Actions


### Create Account
Creates a new account.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| name | Name | STRING | TEXT  |  Name of the account.  |  true  |
| website | Website | STRING | URL  |  Website of the account.  |  false  |
| phone | Phone | STRING | TEXT  |  Phone number of the account.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | NUMBER | NUMBER  |
| name | STRING | TEXT  |
| website | STRING | TEXT  |
| phone | STRING | TEXT  |






### Create Contact
Add new contact in Freshsales CRM.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| first_name | First Name | STRING | TEXT  |  First name of the contact.  |  false  |
| last_name | Last Name | STRING | TEXT  |  Last name of the contact.  |  false  |
| job_title | Job Title | STRING | TEXT  |  Designation of the contact in the account they belong to.  |  false  |
| email | Email | STRING | EMAIL  |  Primary email address of the contact.  |  true  |
| work_number | Work Number | STRING | PHONE  |  Work phone number of the contact.  |  false  |
| mobile_number | Mobile Number | STRING | TEXT  |  Mobile phone number of the contact.  |  false  |
| address | Address | STRING | TEXT  |  Address of the contact.  |  false  |
| city | City | STRING | TEXT  |  City that the contact belongs to.  |  false  |
| state | State | STRING | TEXT  |  State that the contact belongs to.  |  false  |
| zipcode | Zip Code | STRING | TEXT  |  Zipcode of the region that the contact belongs to.  |  false  |
| country | Country | STRING | TEXT  |  Country that the contact belongs to.  |  false  |
| medium | Medium | STRING | TEXT  |  The medium that led your contact to your website/web ap.p  |  false  |
| facebook | Facebook | STRING | TEXT  |  Facebook username of the contact.  |  false  |
| twitter | Twitter | STRING | TEXT  |  Twitter username of the contact.  |  false  |
| linkedin | LinkedIn | STRING | TEXT  |  LinkedIn account of the contact.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | NUMBER | NUMBER  |
| first_name | STRING | TEXT  |
| last_name | STRING | TEXT  |
| job_title | STRING | TEXT  |
| city | STRING | TEXT  |
| state | STRING | TEXT  |
| zipcode | STRING | TEXT  |
| country | STRING | TEXT  |
| email | STRING | TEXT  |
| work_number | STRING | TEXT  |
| mobile_number | STRING | TEXT  |
| address | STRING | TEXT  |
| medium | STRING | TEXT  |
| facebook | STRING | TEXT  |
| twitter | STRING | TEXT  |
| linkedin | STRING | TEXT  |






### Create Lead
Creates a new lead.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| first_name | First Name | STRING | TEXT  |  First name of the lead.  |  false  |
| last_name | Last Name | STRING | TEXT  |  Last name of the lead.  |  false  |
| email | Email | STRING | EMAIL  |  Primary email address of the lead.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | NUMBER | NUMBER  |
| email | STRING | TEXT  |
| first_name | STRING | TEXT  |
| last_name | STRING | TEXT  |








