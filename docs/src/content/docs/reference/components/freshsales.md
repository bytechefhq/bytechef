---
title: "Freshsales"
description: "Freshsales is a customer relationship management (CRM) software designed to help businesses streamline sales processes and manage customer interactions effectively."
---
## Reference
<hr />

Freshsales is a customer relationship management (CRM) software designed to help businesses streamline sales processes and manage customer interactions effectively.


Categories: [CRM]


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


### Create account
Creates a new account

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  Name of the account  |
| Website | STRING | URL  |  Website of the account  |
| Phone | STRING | TEXT  |  Phone number of the account  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| NUMBER | NUMBER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Create contact
Add new contact in Freshsales CRM

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First name | STRING | TEXT  |  First name of the contact  |
| Last name | STRING | TEXT  |  Last name of the contact  |
| Job title | STRING | TEXT  |  Designation of the contact in the account they belong to  |
| Email | STRING | EMAIL  |  Primary email address of the contact  |
| Work number | STRING | PHONE  |  Work phone number of the contact  |
| Mobile number | STRING | TEXT  |  Mobile phone number of the contact  |
| Address | STRING | TEXT  |  Address of the contact  |
| City | STRING | TEXT  |  City that the contact belongs to  |
| State | STRING | TEXT  |  State that the contact belongs to  |
| Zip code | STRING | TEXT  |  Zipcode of the region that the contact belongs to  |
| Country | STRING | TEXT  |  Country that the contact belongs to  |
| Medium | STRING | TEXT  |  The medium that led your contact to your website/web app  |
| Facebook | STRING | TEXT  |  Facebook username of the contact  |
| Twitter | STRING | TEXT  |  Twitter username of the contact  |
| LinkedIn | STRING | TEXT  |  LinkedIn account of the contact  |


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





### Create lead
Creates a new lead

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First name | STRING | TEXT  |  First name of the lead  |
| Last name | STRING | TEXT  |  Last name of the lead  |
| Email | STRING | EMAIL  |  Primary email address of the lead  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| NUMBER | NUMBER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





