---
title: "Google Contacts"
description: "Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms."
---
## Reference
<hr />

Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms.


Categories: [CRM]


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





<hr />





## Actions


### Create contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First name | STRING | TEXT  |  The first name of the contact  |
| Middle name | STRING | TEXT  |  The middle name of the contact  |
| Last name | STRING | TEXT  |  The last name of the contact  |
| Job title | STRING | TEXT  |  The job title of the contact  |
| Company | STRING | TEXT  |  The company of the contact  |
| Email | STRING | EMAIL  |  The email addresses of the contact  |
| Phone number | STRING | PHONE  |  The phone numbers of the contact  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{STRING(firstName), STRING(middleName), STRING(lastName)}] | ARRAY_BUILDER  |
| [{STRING(company), STRING(jobTitle)}] | ARRAY_BUILDER  |
| [{STRING(value)}] | ARRAY_BUILDER  |
| [{STRING(value)}] | ARRAY_BUILDER  |





### Create groups
Creates a new group

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Group name | STRING | TEXT  |  The name of the group  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |





<hr />

# Additional instructions
<hr />

![anl-c-google-contact-md](https://static.scarf.sh/a.png?x-pxid=7efc8d76-26a8-487e-8ca0-0b789556bf64)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

[Guidejar](https://guidejar.com/guides/fec74020-26bb-43dd-814c-f8b907f6f45b) tutorial.

[Turning on Contacts API](https://guidejar.com/guides/0273c3ce-b963-45c0-b7f9-25e893ef060c)
