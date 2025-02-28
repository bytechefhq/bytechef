---
title: "Encharge"
description: "Encharge is a marketing automation platform that helps businesses automate their customer communication and marketing campaigns."
---

Encharge is a marketing automation platform that helps businesses automate their customer communication and marketing campaigns.


Categories: marketing-automation


Type: encharge/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING |  | true |
| value | Value | STRING |  | true |





<hr />



## Actions


### Create Email Template
Name: createEmail

Create email template

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | Name of the email template | true |
| subject | Subject | STRING | Subject of the email | true |
| fromEmail | From Email | STRING | From address to send the email from | true |
| replyEmail | Reply Email | STRING | Address that recipients will reply to by default. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), STRING\(name), STRING\(subject), STRING\(fromEmail), STRING\(replyEmail)}\(email)} </details> |




#### JSON Example
```json
{
  "label" : "Create Email Template",
  "name" : "createEmail",
  "parameters" : {
    "name" : "",
    "subject" : "",
    "fromEmail" : "",
    "replyEmail" : ""
  },
  "type" : "encharge/v1/createEmail"
}
```


### Create People
Name: createPeople

Creates new People

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| people | People | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(website), STRING\(title), STRING\(phone)}] </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(website), STRING\(title), STRING\(id), STRING\(phone)}]\(users)} </details> |




#### JSON Example
```json
{
  "label" : "Create People",
  "name" : "createPeople",
  "parameters" : {
    "people" : [ {
      "email" : "",
      "firstName" : "",
      "lastName" : "",
      "website" : "",
      "title" : "",
      "phone" : ""
    } ]
  },
  "type" : "encharge/v1/createPeople"
}
```


### Add Tag
Name: addTag

Add tag(s) to an existing user.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| tag | Tag | STRING | Tag(s) to add. To add multiple tags, use a comma-separated list, e.g. tag1,tag2 | true |
| email | Email | STRING | Email of the person. | true |


#### JSON Example
```json
{
  "label" : "Add Tag",
  "name" : "addTag",
  "parameters" : {
    "tag" : "",
    "email" : ""
  },
  "type" : "encharge/v1/addTag"
}
```




<hr />

# Additional instructions
<hr />

## CONNECTION

[API Location](https://app.encharge.io/settings/account)
