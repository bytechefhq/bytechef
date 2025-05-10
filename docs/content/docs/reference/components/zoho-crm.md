---
title: "Zoho CRM"
description: "Zoho CRM is a cloud-based customer relationship management platform that integrates sales, marketing, and customer support activities to streamline business processes and enhance team."
---

Zoho CRM is a cloud-based customer relationship management platform that integrates sales, marketing, and customer support activities to streamline business processes and enhance team.


Categories: CRM


Type: zohoCrm/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| region | Region | STRING <details> <summary> Options </summary> zoho.eu, zoho.com, zoho.com.au, zoho.jp, zoho.in, zohocloud.ca </details> |  | true |
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Add User
Name: addUser

Add user to your organization.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| first_name | First Name | STRING | First name of the user. | true |
| last_name | Last Name | STRING | Last name of the user. | false |
| email | Email | STRING | User's email. An invitation will be sent to this email address | true |
| role | Role ID | STRING | ID of the role you want to assign the user with. | true |
| profile | Profile ID | STRING | ID of the profile you want to assign the user with. | true |

#### Example JSON Structure
```json
{
  "label" : "Add User",
  "name" : "addUser",
  "parameters" : {
    "first_name" : "",
    "last_name" : "",
    "email" : "",
    "role" : "",
    "profile" : ""
  },
  "type" : "zohoCrm/v1/addUser"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| users | ARRAY <details> <summary> Items </summary> [{STRING\(code), {STRING\(id)}\(details), STRING\(message), STRING\(status)}] </details> |  |




#### Output Example
```json
{
  "users" : [ {
    "code" : "",
    "details" : {
      "id" : ""
    },
    "message" : "",
    "status" : ""
  } ]
}
```


### Get Organization
Name: getOrganization

Gets information about the current organization.

#### Example JSON Structure
```json
{
  "label" : "Get Organization",
  "name" : "getOrganization",
  "type" : "zohoCrm/v1/getOrganization"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| org | ARRAY <details> <summary> Items </summary> [{STRING\(type), STRING\(id), STRING\(phone), STRING\(company_name), STRING\(primary_email)}] </details> |  |




#### Output Example
```json
{
  "org" : [ {
    "type" : "",
    "id" : "",
    "phone" : "",
    "company_name" : "",
    "primary_email" : ""
  } ]
}
```


### List Users
Name: listUsers

Lists users found in Zoho account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| type | Type | STRING <details> <summary> Options </summary> AllUsers, ActiveUsers, DeactiveUsers, ConfirmedUsers, NotConfirmedUsers, DeletedUsers, ActiveConfirmedUsers, AdminUsers, ActiveConfirmedAdmins, CurrentUser </details> | What type of user to return in list. | true |

#### Example JSON Structure
```json
{
  "label" : "List Users",
  "name" : "listUsers",
  "parameters" : {
    "type" : ""
  },
  "type" : "zohoCrm/v1/listUsers"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| users | ARRAY <details> <summary> Items </summary> [{STRING\(country), STRING\(language), STRING\(id), {STRING\(name), STRING\(id)}\(profile), {STRING\(name), STRING\(id)}\(created_by), STRING\(full_name), STRING\(status), {STRING\(name), STRING\(id)}\(role), STRING\(first_name), STRING\(email)}] </details> |  |




#### Output Example
```json
{
  "users" : [ {
    "country" : "",
    "language" : "",
    "id" : "",
    "profile" : {
      "name" : "",
      "id" : ""
    },
    "created_by" : {
      "name" : "",
      "id" : ""
    },
    "full_name" : "",
    "status" : "",
    "role" : {
      "name" : "",
      "id" : ""
    },
    "first_name" : "",
    "email" : ""
  } ]
}
```




