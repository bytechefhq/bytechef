---
title: "One Simple API"
description: "A toolbox with all the things you need to get your project to success:  Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, Email Validation, QR codes, and much more!"
---

A toolbox with all the things you need to get your project to success:  Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, Email Validation, QR codes, and much more!


Categories: developer-tools


Type: oneSimpleAPI/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | API Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Currency Converter
Convert currency from one to another.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| from_currency | From Currency | STRING | SELECT  |  Currency from which you want to convert.  |  true  |
| to_currency | To Currency | STRING | SELECT  |  Currency to which you want to convert.  |  true  |
| from_value | Value | NUMBER | NUMBER  |  Value to convert.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| from_currency | STRING | TEXT  |
| from_value | STRING | TEXT  |
| to_currency | STRING | TEXT  |
| to_value | NUMBER | NUMBER  |
| to_exchange_rate | STRING | TEXT  |






### URL Shortener
Shorten your desired URL

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| url | URL | STRING | TEXT  |  Place the URL you want to shorten  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| single_use | STRING | TEXT  |
| temporary_redirect | STRING | TEXT  |
| forward_params | STRING | TEXT  |
| short_url | STRING | TEXT  |






### Web Page Information
Get information about a certain webpage

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| url | URL | STRING | TEXT  |  Place the web page url you want to get info from  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| general | {STRING\(title), STRING\(description), STRING\(canonical)} | OBJECT_BUILDER  |
| twitter | {STRING\(site), STRING\(title), STRING\(description)} | OBJECT_BUILDER  |
| og | {STRING\(title), STRING\(url), STRING\(image), STRING\(description), STRING\(type)} | OBJECT_BUILDER  |








