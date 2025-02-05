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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  | true  |
| value | Value | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Email Template
Create email template

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Email Template | {STRING\(name), STRING\(subject), STRING\(fromEmail), STRING\(replyEmail)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), STRING\(name), STRING\(subject), STRING\(fromEmail), STRING\(replyEmail)}\(email)} | OBJECT_BUILDER  |






### Create People
Creates new People

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __items | People | [{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(website), STRING\(title), STRING\(phone)}] | ARRAY_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{STRING\(email), STRING\(firstName), STRING\(lastName), STRING\(website), STRING\(title), STRING\(id), STRING\(phone)}]\(users)} | OBJECT_BUILDER  |






### Add Tag
Add tag(s) to an existing user.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Tag | {STRING\(tag), STRING\(email)} | OBJECT_BUILDER  |  | true  |






## Triggers



<hr />

<hr />

# Additional instructions
<hr />

## CONNECTION

[API Location](https://app.encharge.io/settings/account)
