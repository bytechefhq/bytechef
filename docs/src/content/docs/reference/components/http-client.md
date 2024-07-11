---
title: "HTTP Client"
description: "Makes an HTTP request and returns the response data."
---
## Reference
<hr />

Makes an HTTP request and returns the response data.

Categories: [HELPERS]

Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Value | STRING | TEXT  |
| Add to | STRING | SELECT  |



### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |



### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Username | STRING | TEXT  |
| Password | STRING | TEXT  |



### Digest Auth

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Username | STRING | TEXT  |
| Password | STRING | TEXT  |



### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Authorization URL | STRING | TEXT  |
| Token URL | STRING | TEXT  |
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |
| Header Prefix | STRING | TEXT  |
| Scopes | STRING | TEXT_AREA  |



### OAuth2 Implicit Code

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Authorization URL | STRING | TEXT  |
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |
| Header Prefix | STRING | TEXT  |
| Scopes | STRING | TEXT_AREA  |



### OAuth2 Client Credentials

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token URL | STRING | TEXT  |
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |
| Header Prefix | STRING | TEXT  |
| Scopes | STRING | TEXT_AREA  |





<hr />





## Actions


### GET
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URI | STRING | TEXT  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |
| Response Format | STRING | SELECT  |
| Response Filename | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Query Parameters | OBJECT | OBJECT_BUILDER  |
| Full Response | BOOLEAN | SELECT  |
| Follow All Redirects | BOOLEAN | SELECT  |
| Follow GET Redirect | BOOLEAN | SELECT  |
| Ignore Response Code | BOOLEAN | SELECT  |
| Proxy | STRING | TEXT  |
| Timeout | INTEGER | INTEGER  |




### POST
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URI | STRING | TEXT  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |
| Response Format | STRING | SELECT  |
| Response Filename | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Query Parameters | OBJECT | OBJECT_BUILDER  |
| Body Content Type | STRING | SELECT  |
| Body Content - JSON | OBJECT | OBJECT_BUILDER  |
| Body Content - XML | OBJECT | OBJECT_BUILDER  |
| Body Content - Form Data | OBJECT | OBJECT_BUILDER  |
| Body Content - Form URL-Encoded | OBJECT | OBJECT_BUILDER  |
| Body Content - Raw | STRING | TEXT  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |
| Content Type | STRING | TEXT  |
| Full Response | BOOLEAN | SELECT  |
| Follow All Redirects | BOOLEAN | SELECT  |
| Follow GET Redirect | BOOLEAN | SELECT  |
| Ignore Response Code | BOOLEAN | SELECT  |
| Proxy | STRING | TEXT  |
| Timeout | INTEGER | INTEGER  |




### PUT
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URI | STRING | TEXT  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |
| Response Format | STRING | SELECT  |
| Response Filename | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Query Parameters | OBJECT | OBJECT_BUILDER  |
| Body Content - JSON | OBJECT | OBJECT_BUILDER  |
| Body Content - XML | OBJECT | OBJECT_BUILDER  |
| Body Content - Form Data | OBJECT | OBJECT_BUILDER  |
| Body Content - Form URL-Encoded | OBJECT | OBJECT_BUILDER  |
| Body Content - Raw | STRING | TEXT  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |
| Body Content Type | STRING | SELECT  |
| Body Content - JSON | OBJECT | OBJECT_BUILDER  |
| Body Content - XML | OBJECT | OBJECT_BUILDER  |
| Body Content - Form Data | OBJECT | OBJECT_BUILDER  |
| Body Content - Form URL-Encoded | OBJECT | OBJECT_BUILDER  |
| Body Content - Raw | STRING | TEXT  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |
| Content Type | STRING | TEXT  |
| Full Response | BOOLEAN | SELECT  |
| Follow All Redirects | BOOLEAN | SELECT  |
| Follow GET Redirect | BOOLEAN | SELECT  |
| Ignore Response Code | BOOLEAN | SELECT  |
| Proxy | STRING | TEXT  |
| Timeout | INTEGER | INTEGER  |




### PATCH
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URI | STRING | TEXT  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |
| Response Format | STRING | SELECT  |
| Response Filename | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Query Parameters | OBJECT | OBJECT_BUILDER  |
| Body Content - JSON | OBJECT | OBJECT_BUILDER  |
| Body Content - XML | OBJECT | OBJECT_BUILDER  |
| Body Content - Form Data | OBJECT | OBJECT_BUILDER  |
| Body Content - Form URL-Encoded | OBJECT | OBJECT_BUILDER  |
| Body Content - Raw | STRING | TEXT  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |
| Body Content Type | STRING | SELECT  |
| Body Content - JSON | OBJECT | OBJECT_BUILDER  |
| Body Content - XML | OBJECT | OBJECT_BUILDER  |
| Body Content - Form Data | OBJECT | OBJECT_BUILDER  |
| Body Content - Form URL-Encoded | OBJECT | OBJECT_BUILDER  |
| Body Content - Raw | STRING | TEXT  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |
| Content Type | STRING | TEXT  |
| Full Response | BOOLEAN | SELECT  |
| Follow All Redirects | BOOLEAN | SELECT  |
| Follow GET Redirect | BOOLEAN | SELECT  |
| Ignore Response Code | BOOLEAN | SELECT  |
| Proxy | STRING | TEXT  |
| Timeout | INTEGER | INTEGER  |




### DELETE
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URI | STRING | TEXT  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |
| Response Format | STRING | SELECT  |
| Response Filename | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Query Parameters | OBJECT | OBJECT_BUILDER  |
| Full Response | BOOLEAN | SELECT  |
| Follow All Redirects | BOOLEAN | SELECT  |
| Follow GET Redirect | BOOLEAN | SELECT  |
| Ignore Response Code | BOOLEAN | SELECT  |
| Proxy | STRING | TEXT  |
| Timeout | INTEGER | INTEGER  |




### HEAD
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| URI | STRING | TEXT  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |
| Response Format | STRING | SELECT  |
| Response Filename | STRING | TEXT  |
| Headers | OBJECT | OBJECT_BUILDER  |
| Query Parameters | OBJECT | OBJECT_BUILDER  |
| Full Response | BOOLEAN | SELECT  |
| Follow All Redirects | BOOLEAN | SELECT  |
| Follow GET Redirect | BOOLEAN | SELECT  |
| Ignore Response Code | BOOLEAN | SELECT  |
| Proxy | STRING | TEXT  |
| Timeout | INTEGER | INTEGER  |




