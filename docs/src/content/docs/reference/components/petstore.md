---
title: "Petstore"
description: "This is a sample Pet Store Server based on the OpenAPI 3.0 specification."
---
## Reference
<hr />

This is a sample Pet Store Server based on the OpenAPI 3.0 specification.

Categories: null

Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Implicit

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Client Id | STRING | TEXT  |
| Client Secret | STRING | TEXT  |



### API Key

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Key | STRING | TEXT  |
| Value | STRING | TEXT  |





<hr />



## Triggers



<hr />



## Actions


### Add a new pet to the store
Add a new pet to the store

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Pet | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Name | STRING | TEXT  |
| Category | OBJECT | OBJECT_BUILDER  |
| Photo Urls | ARRAY | ARRAY_BUILDER  |
| Tags | ARRAY | ARRAY_BUILDER  |
| Status | STRING | SELECT  |





### Update an existing pet
Update an existing pet by Id

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Pet | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Name | STRING | TEXT  |
| Category | OBJECT | OBJECT_BUILDER  |
| Photo Urls | ARRAY | ARRAY_BUILDER  |
| Tags | ARRAY | ARRAY_BUILDER  |
| Status | STRING | SELECT  |





### Finds Pets by status
Multiple status values can be provided with comma separated strings

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Status | STRING | SELECT  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Finds Pets by tags
Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Tags | ARRAY | ARRAY_BUILDER  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Deletes a pet
delete a pet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Api Key | STRING | TEXT  |
| Pet Id | INTEGER | INTEGER  |




### Find pet by ID
Returns a single pet

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Pet Id | INTEGER | INTEGER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Name | STRING | TEXT  |
| Category | OBJECT | OBJECT_BUILDER  |
| Photo Urls | ARRAY | ARRAY_BUILDER  |
| Tags | ARRAY | ARRAY_BUILDER  |
| Status | STRING | SELECT  |





### Updates a pet in the store with form data


#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Pet Id | INTEGER | INTEGER  |
| Name | STRING | TEXT  |
| Status | STRING | TEXT  |




### uploads an image


#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Pet Id | INTEGER | INTEGER  |
| Additional Metadata | STRING | TEXT  |
| FILE_ENTRY | FILE_ENTRY  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Code | INTEGER | INTEGER  |
| Type | STRING | TEXT  |
| Message | STRING | TEXT  |





### Returns pet inventories by status
Returns a map of status codes to quantities

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|



### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Place an order for a pet
Place a new order in the store

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Order | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Pet Id | INTEGER | INTEGER  |
| Quantity | INTEGER | INTEGER  |
| Ship Date | DATE_TIME | DATE_TIME  |
| Status | STRING | SELECT  |
| Complete | BOOLEAN | SELECT  |





### Delete purchase order by ID
For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Order Id | INTEGER | INTEGER  |




### Find purchase order by ID
For valid response try integer IDs with value <= 5 or > 10. Other values will generate exceptions.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Order Id | INTEGER | INTEGER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Pet Id | INTEGER | INTEGER  |
| Quantity | INTEGER | INTEGER  |
| Ship Date | DATE_TIME | DATE_TIME  |
| Status | STRING | SELECT  |
| Complete | BOOLEAN | SELECT  |





### Create user
This can only be done by the logged in user.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| User | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Username | STRING | TEXT  |
| First Name | STRING | TEXT  |
| Last Name | STRING | TEXT  |
| Email | STRING | TEXT  |
| Password | STRING | TEXT  |
| Phone | STRING | TEXT  |
| User Status | INTEGER | INTEGER  |





### Creates list of users with given input array
Creates list of users with given input array

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Items | ARRAY | ARRAY_BUILDER  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Delete user
This can only be done by the logged in user.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Username | STRING | TEXT  |




### Get user by user name


#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Username | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Username | STRING | TEXT  |
| First Name | STRING | TEXT  |
| Last Name | STRING | TEXT  |
| Email | STRING | TEXT  |
| Password | STRING | TEXT  |
| Phone | STRING | TEXT  |
| User Status | INTEGER | INTEGER  |





### Update user
This can only be done by the logged in user.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Username | STRING | TEXT  |
| User | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |
| Username | STRING | TEXT  |
| First Name | STRING | TEXT  |
| Last Name | STRING | TEXT  |
| Email | STRING | TEXT  |
| Password | STRING | TEXT  |
| Phone | STRING | TEXT  |
| User Status | INTEGER | INTEGER  |





