---
title: "Freshsales"
description: "Freshsales is a customer relationship management (CRM) software designed to help businesses streamline sales processes and manage customer interactions effectively."
---
## Reference
<hr />

Freshsales is a customer relationship management (CRM) software designed to help businesses streamline sales processes and manage customer interactions effectively.


Categories: [crm]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Bundle alias | STRING | TEXT  |  Your Freshsales bundle alias (e.g. https://<alias>.myfreshworks.com)  |
| API Key | STRING | TEXT  |  The API Key supplied by Freshsales  |





<hr />





## Actions


### Create Account
Creates a new account.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Name of the account.  |
| Website | STRING | URL  |  Website of the account.  |
| Phone | STRING | TEXT  |  Phone number of the account.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| NUMBER | NUMBER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create Contact
Add new contact in Freshsales CRM.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First Name | STRING | TEXT  |  First name of the contact.  |
| Last Name | STRING | TEXT  |  Last name of the contact.  |
| Job Title | STRING | TEXT  |  Designation of the contact in the account they belong to.  |
| Email | STRING | EMAIL  |  Primary email address of the contact.  |
| Work Number | STRING | PHONE  |  Work phone number of the contact.  |
| Mobile Number | STRING | TEXT  |  Mobile phone number of the contact.  |
| Address | STRING | TEXT  |  Address of the contact.  |
| City | STRING | TEXT  |  City that the contact belongs to.  |
| State | STRING | TEXT  |  State that the contact belongs to.  |
| Zip Code | STRING | TEXT  |  Zipcode of the region that the contact belongs to.  |
| Country | STRING | TEXT  |  Country that the contact belongs to.  |
| Medium | STRING | TEXT  |  The medium that led your contact to your website/web ap.p  |
| Facebook | STRING | TEXT  |  Facebook username of the contact.  |
| Twitter | STRING | TEXT  |  Twitter username of the contact.  |
| LinkedIn | STRING | TEXT  |  LinkedIn account of the contact.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| NUMBER | NUMBER  |
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
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Create Lead
Creates a new lead.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First Name | STRING | TEXT  |  First name of the lead.  |
| Last Name | STRING | TEXT  |  Last name of the lead.  |
| Email | STRING | EMAIL  |  Primary email address of the lead.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| NUMBER | NUMBER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






