---
title: "One Simple API"
description: "A toolbox with all the things you need to get your project to success:  Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, Email Validation, QR codes, and much more!"
---
## Reference
<hr />

A toolbox with all the things you need to get your project to success:  Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, Email Validation, QR codes, and much more!


Categories: [DEVELOPER_TOOLS]


Version: 1

<hr />



## Connections

Version: 1


### One Simple API Connection

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Access Token | STRING | TEXT  |





<hr />





## Actions


### Currency Converter
Convert your currency into any other

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| From Currency | STRING | SELECT  |
| To Currency | STRING | SELECT  |
| Value | NUMBER | NUMBER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### URL Shortener
Shorten your desired URL

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URL | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |





### Web Page Information
Get information about a certain webpage

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URL | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





