---
title: "Encharge"
description: "Encharge is a marketing automation platform that helps businesses automate their customer communication and marketing campaigns."
---
## Reference
<hr />

Encharge is a marketing automation platform that helps businesses automate their customer communication and marketing campaigns.


Categories: [MARKETING_AUTOMATION]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  |
| Value | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Create email template
Create email template

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Email   Template | {STRING(name), STRING(subject), STRING(fromEmail), STRING(replyEmail)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER(id), STRING(name), STRING(subject), STRING(fromEmail), STRING(replyEmail)}(email)} | OBJECT_BUILDER  |





### Create people
Creates new People

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| People | [{STRING(email), STRING(firstName), STRING(lastName), STRING(website), STRING(title), STRING(phone)}] | ARRAY_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{STRING(email), STRING(firstName), STRING(lastName), STRING(website), STRING(title), STRING(id), STRING(phone)}](users)} | OBJECT_BUILDER  |





### Add tag
Add tag(s) to an existing user.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Tag | {STRING(tag), STRING(email)} | OBJECT_BUILDER  |  |




<hr />

# Additional instructions
<hr />

## CONNECTION

[API Location](https://app.encharge.io/settings/account)
