---
title: "Google Contacts"
description: "Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms."
---
## Reference
<hr />

Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms.


Categories: [crm]


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


### Create Contact
Creates a new contact..

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| First Name | STRING | TEXT  |  The first name of the contact.  |
| Middle Name | STRING | TEXT  |  The middle name of the contact.  |
| Last Name | STRING | TEXT  |  The last name of the contact.  |
| Job Title | STRING | TEXT  |  The job title of the contact.  |
| Company | STRING | TEXT  |  The company of the contact.  |
| Email | STRING | EMAIL  |  The email addresses of the contact.  |
| Phone Number | STRING | PHONE  |  The phone numbers of the contact.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{STRING\(firstName), STRING\(middleName), STRING\(lastName)}] | ARRAY_BUILDER  |
| [{STRING\(company), STRING\(jobTitle)}] | ARRAY_BUILDER  |
| [{STRING\(value)}] | ARRAY_BUILDER  |
| [{STRING\(value)}] | ARRAY_BUILDER  |






### Create Group
Creates a new group.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Group Name | STRING | TEXT  |  The name of the group.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |






### Update Contact
Modifies an existing contact.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Resource Name | STRING | TEXT  |  Resource name of the contact to be updated.  |
| First Name | STRING | TEXT  |  New first name of the contact.  |
| Middle Name | STRING | TEXT  |  New middle name of the contact.  |
| Last Name | STRING | TEXT  |  Updated last name of the contact.  |
| Job Title | STRING | TEXT  |  Updated job title of the contact.  |
| Company | STRING | TEXT  |  Updated name of the company where the contact is employed.  |
| Email Address | STRING | EMAIL  |  Updated email address of the contact.  |
| Phone Number | STRING | PHONE  |  Updated phone number of the contact.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| [{STRING\(firstName), STRING\(middleName), STRING\(lastName)}] | ARRAY_BUILDER  |
| [{STRING\(company), STRING\(jobTitle)}] | ARRAY_BUILDER  |
| [{STRING\(value)}] | ARRAY_BUILDER  |
| [{STRING\(value)}] | ARRAY_BUILDER  |






<hr />

# Additional instructions
<hr />

![anl-c-google-contact-md](https://static.scarf.sh/a.png?x-pxid=7efc8d76-26a8-487e-8ca0-0b789556bf64)
## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Contacts API <div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/0273c3ce-b963-45c0-b7f9-25e893ef060c?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
