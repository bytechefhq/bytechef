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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| givenName | First Name | STRING | The first name of the contact. | true |
| middleName | Middle Name | STRING | The middle name of the contact. | false |
| familyName | Last Name | STRING | The last name of the contact. | true |
| title | Job Title | STRING | The job title of the contact. | false |
| name | Company | STRING | The company of the contact. | false |
| email | Email | STRING | The email addresses of the contact. | false |
| phoneNumber | Phone Number | STRING | The phone numbers of the contact. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| names | ARRAY <details> <summary> Items </summary> [{STRING\(familyName), STRING\(givenName), STRING\(middleName)}] </details> |
| organizations | ARRAY <details> <summary> Items </summary> [{STRING\(name), STRING\(title)}] </details> |
| emailAddresses | ARRAY <details> <summary> Items </summary> [{STRING\(value)}] </details> |
| phoneNumbers | ARRAY <details> <summary> Items </summary> [{STRING\(value)}] </details> |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "givenName" : "",
    "middleName" : "",
    "familyName" : "",
    "title" : "",
    "name" : "",
    "email" : "",
    "phoneNumber" : ""
  },
  "type" : "googleContacts/v1/createContact"
}
```


### Create Group
Name: createGroup

Creates a new group.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Group Name | STRING | The name of the group. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| name | STRING |




#### JSON Example
```json
{
  "label" : "Create Group",
  "name" : "createGroup",
  "parameters" : {
    "name" : ""
  },
  "type" : "googleContacts/v1/createGroup"
}
```


### Update Contact
Name: updateContact

Modifies an existing contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| resourceName | Resource Name | STRING | Resource name of the contact to be updated. | true |
| givenName | First Name | STRING | New first name of the contact. | true |
| middleName | Middle Name | STRING | New middle name of the contact. | false |
| familyName | Last Name | STRING | Updated last name of the contact. | true |
| title | Job Title | STRING | Updated job title of the contact. | false |
| name | Company | STRING | Updated name of the company where the contact is employed. | false |
| email | Email Address | STRING | Updated email address of the contact. | false |
| phoneNumber | Phone Number | STRING | Updated phone number of the contact. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| names | ARRAY <details> <summary> Items </summary> [{STRING\(familyName), STRING\(givenName), STRING\(middleName)}] </details> |
| organizations | ARRAY <details> <summary> Items </summary> [{STRING\(name), STRING\(title)}] </details> |
| emailAddresses | ARRAY <details> <summary> Items </summary> [{STRING\(value)}] </details> |
| phoneNumbers | ARRAY <details> <summary> Items </summary> [{STRING\(value)}] </details> |




#### JSON Example
```json
{
  "label" : "Update Contact",
  "name" : "updateContact",
  "parameters" : {
    "resourceName" : "",
    "givenName" : "",
    "middleName" : "",
    "familyName" : "",
    "title" : "",
    "name" : "",
    "email" : "",
    "phoneNumber" : ""
  },
  "type" : "googleContacts/v1/updateContact"
}
```


### Search Contacts
Name: searchContacts

Searches the contacts in Google Contacts account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| query | Query | STRING | The plain-text query. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {[{STRING\(familyName), STRING\(givenName), STRING\(middleName)}]\(names), [{STRING\(name), STRING\(title)}]\(organizations), [{STRING\(value)}]\(emailAddresses), [{STRING\(value)}]\(phoneNumbers)} </details> |




#### JSON Example
```json
{
  "label" : "Search Contacts",
  "name" : "searchContacts",
  "parameters" : {
    "query" : ""
  },
  "type" : "googleContacts/v1/searchContacts"
}
```




<hr />

# Additional instructions
<hr />

![anl-c-google-contact-md](https://static.scarf.sh/a.png?x-pxid=7efc8d76-26a8-487e-8ca0-0b789556bf64)

## CONNECTION

[Setting up OAuth2](https://support.google.com/googleapi/answer/6158849?hl=en)

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/fec74020-26bb-43dd-814c-f8b907f6f45b?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

Turning on Contacts API
<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)"><iframe src="https://www.guidejar.com/embed/0273c3ce-b963-45c0-b7f9-25e893ef060c?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
