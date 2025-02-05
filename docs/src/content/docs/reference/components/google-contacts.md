---
title: "Google Contacts"
description: "Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms."
---

Google Contacts is a cloud-based address book service provided by Google, allowing users to store, manage, and synchronize their contact information across multiple devices and platforms.


Categories: crm


Type: googleContacts/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Contact
Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| givenName | First Name | STRING | TEXT  |  The first name of the contact.  |  true  |
| middleName | Middle Name | STRING | TEXT  |  The middle name of the contact.  |  false  |
| familyName | Last Name | STRING | TEXT  |  The last name of the contact.  |  true  |
| title | Job Title | STRING | TEXT  |  The job title of the contact.  |  false  |
| name | Company | STRING | TEXT  |  The company of the contact.  |  false  |
| email | Email | STRING | EMAIL  |  The email addresses of the contact.  |  false  |
| phoneNumber | Phone Number | STRING | PHONE  |  The phone numbers of the contact.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| names | [{STRING\(familyName), STRING\(givenName), STRING\(middleName)}] | ARRAY_BUILDER  |
| organizations | [{STRING\(name), STRING\(title)}] | ARRAY_BUILDER  |
| emailAddresses | [{STRING\(value)}] | ARRAY_BUILDER  |
| phoneNumbers | [{STRING\(value)}] | ARRAY_BUILDER  |






### Create Group
Creates a new group.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| name | Group Name | STRING | TEXT  |  The name of the group.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| name | STRING | TEXT  |






### Update Contact
Modifies an existing contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| resourceName | Resource Name | STRING | TEXT  |  Resource name of the contact to be updated.  |  true  |
| givenName | First Name | STRING | TEXT  |  New first name of the contact.  |  true  |
| middleName | Middle Name | STRING | TEXT  |  New middle name of the contact.  |  false  |
| familyName | Last Name | STRING | TEXT  |  Updated last name of the contact.  |  true  |
| title | Job Title | STRING | TEXT  |  Updated job title of the contact.  |  false  |
| name | Company | STRING | TEXT  |  Updated name of the company where the contact is employed.  |  false  |
| email | Email Address | STRING | EMAIL  |  Updated email address of the contact.  |  false  |
| phoneNumber | Phone Number | STRING | PHONE  |  Updated phone number of the contact.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| names | [{STRING\(familyName), STRING\(givenName), STRING\(middleName)}] | ARRAY_BUILDER  |
| organizations | [{STRING\(name), STRING\(title)}] | ARRAY_BUILDER  |
| emailAddresses | [{STRING\(value)}] | ARRAY_BUILDER  |
| phoneNumbers | [{STRING\(value)}] | ARRAY_BUILDER  |






### Search Contacts
Searches the contacts in Google Contacts account.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| query | Query | STRING | TEXT  |  The plain-text query.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {[{STRING\(familyName), STRING\(givenName), STRING\(middleName)}]\(names), [{STRING\(name), STRING\(title)}]\(organizations), [{STRING\(value)}]\(emailAddresses), [{STRING\(value)}]\(phoneNumbers)} | OBJECT_BUILDER  |








<hr />

# Additional instructions
<hr />

![anl-c-google-contact-md](https://static.scarf.sh/a.png?x-pxid=7efc8d76-26a8-487e-8ca0-0b789556bf64)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Contacts API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/0273c3ce-b963-45c0-b7f9-25e893ef060c?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
