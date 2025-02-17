---
title: "Acumbamail"
description: "Acumbamail is an email marketing and automation platform that allows users to create, manage, and analyze email campaigns, newsletters, and SMS marketing with an intuitive interface and API integration."
---

Acumbamail is an email marketing and automation platform that allows users to create, manage, and analyze email campaigns, newsletters, and SMS marketing with an intuitive interface and API integration.


Categories: marketing-automation


Type: acumbamail/v1

<hr />



## Connections

Version: 1


### Authorization token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| access_token | Access token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Add Subscriber
Name: addSubscriber

Add a subscriber to a list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| list_id | List Id | INTEGER | SELECT  |  List identifier.  |  true  |


#### Output



Type: INTEGER







### Delete Subscriber
Name: deleteSubscriber

Removes a subscriber from a list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| list_id | List Id | INTEGER | SELECT  |  List identifier.  |  true  |
| email | Email | STRING | SELECT  |  Subscriber email address.  |  true  |




### Create Subscriber List
Name: createSubscriberList

Creates a new subscribers list.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| email | Email | STRING | TEXT  |  Email address that will be used for list notifications.  |  true  |
| name | Name | STRING | TEXT  |  List name  |  true  |
| company | Company | STRING | TEXT  |  Company that the list belongs to  |  false  |
| country | Country | STRING | TEXT  |  Country where the list comes from  |  false  |
| city | City | STRING | TEXT  |  City of the company  |  false  |
| address | Address | STRING | TEXT  |  Address of the company  |  false  |
| phone | Phone | STRING | TEXT  |  Phone number of the company  |  false  |


#### Output



Type: INTEGER







### Delete Subscriber List
Name: deleteSubscriberList

Deletes a list of subscribers.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| list_id | List Id | INTEGER | SELECT  |  List identifier.  |  true  |






