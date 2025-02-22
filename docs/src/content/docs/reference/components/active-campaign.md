---
title: "ActiveCampaign"
description: "ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools."
---

ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools.


Categories: crm, marketing-automation


Type: active-campaign/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Account name | STRING | Your account name, e.g. https://{youraccountname}.api-us1.com | true |
| key | Key | STRING |  | true |
| value | API Key | STRING |  | true |





<hr />



## Actions


### Create Account
Name: createAccount

Creates a new account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Account | OBJECT <details> <summary> Properties </summary> {{STRING\(name), STRING\(accountUrl)}\(account)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(name), STRING\(accountUrl)}\(account)} </details> |




#### JSON Example
```json
{
  "label" : "Create Account",
  "name" : "createAccount",
  "parameters" : {
    "__item" : {
      "account" : {
        "name" : "",
        "accountUrl" : ""
      }
    }
  },
  "type" : "active-campaign/v1/createAccount"
}
```


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Contact | OBJECT <details> <summary> Properties </summary> {{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(phone)}\(contact)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(phone)}\(contact)} </details> |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
      "contact" : {
        "email" : "",
        "firstName" : "",
        "lastName" : "",
        "phone" : ""
      }
    }
  },
  "type" : "active-campaign/v1/createContact"
}
```


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __item | Task | OBJECT <details> <summary> Properties </summary> {{STRING\(title), INTEGER\(relid), DATE\(duedate), INTEGER\(dealTasktype)}\(dealTask)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(title), INTEGER\(relid), DATE\(duedate), INTEGER\(dealTasktype)}\(dealTask)} </details> |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "__item" : {
      "dealTask" : {
        "title" : "",
        "relid" : 1,
        "duedate" : "2021-01-01",
        "dealTasktype" : 1
      }
    }
  },
  "type" : "active-campaign/v1/createTask"
}
```




