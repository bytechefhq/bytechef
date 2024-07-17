---
title: "Petstore"
description: "This is a sample Pet Store Server based on the OpenAPI 3.0 specification."
---
## Reference
<hr />

This is a sample Pet Store Server based on the OpenAPI 3.0 specification.



Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Implicit

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |



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


### Add a new pet to the store
Add a new pet to the store

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Pet | {INTEGER(id), STRING(name), {INTEGER(id), STRING(name)}(category), [STRING](photoUrls), [{INTEGER(id), STRING(name)}](tags), STRING(status)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Name | STRING | TEXT  |  |
| Category | {INTEGER(id), STRING(name)} | OBJECT_BUILDER  |  |
| Photo Urls | [STRING] | ARRAY_BUILDER  |  |
| Tags | [{INTEGER(id), STRING(name)}] | ARRAY_BUILDER  |  |
| Status | STRING | SELECT  |  pet status in the store  |





### Update an existing pet
Update an existing pet by Id

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Pet | {INTEGER(id), STRING(name), {INTEGER(id), STRING(name)}(category), [STRING](photoUrls), [{INTEGER(id), STRING(name)}](tags), STRING(status)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Name | STRING | TEXT  |  |
| Category | {INTEGER(id), STRING(name)} | OBJECT_BUILDER  |  |
| Photo Urls | [STRING] | ARRAY_BUILDER  |  |
| Tags | [{INTEGER(id), STRING(name)}] | ARRAY_BUILDER  |  |
| Status | STRING | SELECT  |  pet status in the store  |





### Finds Pets by status
Multiple status values can be provided with comma separated strings

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Status | STRING | SELECT  |  Status values that need to be considered for filter  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Finds Pets by tags
Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Tags | [STRING] | ARRAY_BUILDER  |  Tags to filter by  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Deletes a pet
delete a pet

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Api Key | STRING | TEXT  |    |
| Pet Id | INTEGER | INTEGER  |  Pet id to delete  |




### Find pet by ID
Returns a single pet

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Pet Id | INTEGER | INTEGER  |  ID of pet to return  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Name | STRING | TEXT  |  |
| Category | {INTEGER(id), STRING(name)} | OBJECT_BUILDER  |  |
| Photo Urls | [STRING] | ARRAY_BUILDER  |  |
| Tags | [{INTEGER(id), STRING(name)}] | ARRAY_BUILDER  |  |
| Status | STRING | SELECT  |  pet status in the store  |





### Updates a pet in the store with form data


#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Pet Id | INTEGER | INTEGER  |  ID of pet that needs to be updated  |
| Name | STRING | TEXT  |  Name of pet that needs to be updated  |
| Status | STRING | TEXT  |  Status of pet that needs to be updated  |




### uploads an image


#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Pet Id | INTEGER | INTEGER  |  ID of pet to update  |
| Additional Metadata | STRING | TEXT  |  Additional Metadata  |
| FILE_ENTRY | FILE_ENTRY  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Code | INTEGER | INTEGER  |  |
| Type | STRING | TEXT  |  |
| Message | STRING | TEXT  |  |





### Returns pet inventories by status
Returns a map of status codes to quantities

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|



### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Place an order for a pet
Place a new order in the store

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order | {INTEGER(id), INTEGER(petId), INTEGER(quantity), DATE_TIME(shipDate), STRING(status), BOOLEAN(complete)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Pet Id | INTEGER | INTEGER  |  |
| Quantity | INTEGER | INTEGER  |  |
| Ship Date | DATE_TIME | DATE_TIME  |  |
| Status | STRING | SELECT  |  Order Status  |
| Complete | BOOLEAN | SELECT  |  |





### Delete purchase order by ID
For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order Id | INTEGER | INTEGER  |  ID of the order that needs to be deleted  |




### Find purchase order by ID
For valid response try integer IDs with value <= 5 or > 10. Other values will generate exceptions.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Order Id | INTEGER | INTEGER  |  ID of order that needs to be fetched  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Pet Id | INTEGER | INTEGER  |  |
| Quantity | INTEGER | INTEGER  |  |
| Ship Date | DATE_TIME | DATE_TIME  |  |
| Status | STRING | SELECT  |  Order Status  |
| Complete | BOOLEAN | SELECT  |  |





### Create user
This can only be done by the logged in user.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| User | {INTEGER(id), STRING(username), STRING(firstName), STRING(lastName), STRING(email), STRING(password), STRING(phone), INTEGER(userStatus)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Username | STRING | TEXT  |  |
| First Name | STRING | TEXT  |  |
| Last Name | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| User Status | INTEGER | INTEGER  |  User Status  |





### Creates list of users with given input array
Creates list of users with given input array

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Items | [{INTEGER(id), STRING(username), STRING(firstName), STRING(lastName), STRING(email), STRING(password), STRING(phone), INTEGER(userStatus)}] | ARRAY_BUILDER  |  |


### Output



Type: ARRAY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
null





### Delete user
This can only be done by the logged in user.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  The name that needs to be deleted  |




### Get user by user name


#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  The name that needs to be fetched. Use user1 for testing.   |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Username | STRING | TEXT  |  |
| First Name | STRING | TEXT  |  |
| Last Name | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| User Status | INTEGER | INTEGER  |  User Status  |





### Update user
This can only be done by the logged in user.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  name that need to be deleted  |
| User | {INTEGER(id), STRING(username), STRING(firstName), STRING(lastName), STRING(email), STRING(password), STRING(phone), INTEGER(userStatus)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| Id | INTEGER | INTEGER  |  |
| Username | STRING | TEXT  |  |
| First Name | STRING | TEXT  |  |
| Last Name | STRING | TEXT  |  |
| Email | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |
| Phone | STRING | TEXT  |  |
| User Status | INTEGER | INTEGER  |  User Status  |





