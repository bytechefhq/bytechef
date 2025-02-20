---
title: "Salesflare"
description: "Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently."
---

Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently.


Categories: crm


Type: salesflare/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | Token | STRING | TEXT |  | true |





<hr />



## Actions


### Create Account
Name: createAccount

Creates new account.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Account | OBJECT <details> <summary> Properties </summary> {STRING\(name), STRING\(website), STRING\(description), STRING\(email), STRING\(phone_number), [STRING]\(social_profiles)} </details> | OBJECT_BUILDER |  | true |


#### JSON Example
```json
{
  "label" : "Create Account",
  "name" : "createAccount",
  "parameters" : {
    "__item" : {
      "name" : "",
      "website" : "",
      "description" : "",
      "email" : "",
      "phone_number" : "",
      "social_profiles" : [ "" ]
    }
  },
  "type" : "salesflare/v1/createAccount"
}
```


### Create Contacts
Name: createContacts

Creates new contacts.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __items | Contacts | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(firstname), STRING\(lastname), STRING\(phone_number), STRING\(mobile_phone_number), STRING\(home_phone_number), STRING\(fax_number), [STRING]\(social_profiles)}] </details> | ARRAY_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | ARRAY <details> <summary> Items </summary> [{INTEGER\(id)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Contacts",
  "name" : "createContacts",
  "parameters" : {
    "__items" : [ {
      "email" : "",
      "firstname" : "",
      "lastname" : "",
      "phone_number" : "",
      "mobile_phone_number" : "",
      "home_phone_number" : "",
      "fax_number" : "",
      "social_profiles" : [ "" ]
    } ]
  },
  "type" : "salesflare/v1/createContacts"
}
```


### Create Tasks
Name: createTasks

Creates new tasks.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __items | Tasks | ARRAY <details> <summary> Items </summary> [{STRING\(description), DATE\(reminder_date)}] </details> | ARRAY_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | ARRAY <details> <summary> Items </summary> [{INTEGER\(id)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Create Tasks",
  "name" : "createTasks",
  "parameters" : {
    "__items" : [ {
      "description" : "",
      "reminder_date" : "2021-01-01"
    } ]
  },
  "type" : "salesflare/v1/createTasks"
}
```




