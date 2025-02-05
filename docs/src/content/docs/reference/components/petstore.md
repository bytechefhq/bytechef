---
title: "Petstore"
description: "This is a sample Pet Store Server based on the OpenAPI 3.0 specification."
---

This is a sample Pet Store Server based on the OpenAPI 3.0 specification.



Type: petstore/v1

<hr />



## Connections

Version: 1


### OAuth2 Implicit

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |



### API Key

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  | true  |
| value | Value | STRING | TEXT  |  | true  |





<hr />



## Actions


### Add a new pet to the store
Add a new pet to the store

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| pet | Pet | {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| name | Name | STRING | TEXT  |  | true  |
| category | Category | {INTEGER\(id), STRING\(name)} | OBJECT_BUILDER  |  | false  |
| photoUrls | Photo Urls | [STRING] | ARRAY_BUILDER  |  | true  |
| tags | Tags | [{INTEGER\(id), STRING\(name)}] | ARRAY_BUILDER  |  | false  |
| status | Status | STRING | SELECT  |  pet status in the store  |  false  |






### Update an existing pet
Update an existing pet by Id

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| pet | Pet | {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} | OBJECT_BUILDER  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| name | Name | STRING | TEXT  |  | true  |
| category | Category | {INTEGER\(id), STRING\(name)} | OBJECT_BUILDER  |  | false  |
| photoUrls | Photo Urls | [STRING] | ARRAY_BUILDER  |  | true  |
| tags | Tags | [{INTEGER\(id), STRING\(name)}] | ARRAY_BUILDER  |  | false  |
| status | Status | STRING | SELECT  |  pet status in the store  |  false  |






### Finds Pets by status
Multiple status values can be provided with comma separated strings

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| status | Status | STRING | SELECT  |  Status values that need to be considered for filter  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} | OBJECT_BUILDER  |






### Finds Pets by tags
Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tags | Tags | [STRING] | ARRAY_BUILDER  |  Tags to filter by  |  false  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} | OBJECT_BUILDER  |






### Deletes a pet
delete a pet

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| api_key | Api Key | STRING | TEXT  |    |  false  |
| petId | Pet Id | INTEGER | INTEGER  |  Pet id to delete  |  true  |




### Find pet by ID
Returns a single pet

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| petId | Pet Id | INTEGER | INTEGER  |  ID of pet to return  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| name | Name | STRING | TEXT  |  | true  |
| category | Category | {INTEGER\(id), STRING\(name)} | OBJECT_BUILDER  |  | false  |
| photoUrls | Photo Urls | [STRING] | ARRAY_BUILDER  |  | true  |
| tags | Tags | [{INTEGER\(id), STRING\(name)}] | ARRAY_BUILDER  |  | false  |
| status | Status | STRING | SELECT  |  pet status in the store  |  false  |






### Updates a pet in the store with form data


#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| petId | Pet Id | INTEGER | INTEGER  |  ID of pet that needs to be updated  |  true  |
| name | Name | STRING | TEXT  |  Name of pet that needs to be updated  |  false  |
| status | Status | STRING | TEXT  |  Status of pet that needs to be updated  |  false  |




### uploads an image


#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| petId | Pet Id | INTEGER | INTEGER  |  ID of pet to update  |  true  |
| additionalMetadata | Additional Metadata | STRING | TEXT  |  Additional Metadata  |  false  |
| fileEntry | FILE_ENTRY | FILE_ENTRY  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| code | Code | INTEGER | INTEGER  |  | false  |
| type | Type | STRING | TEXT  |  | false  |
| message | Message | STRING | TEXT  |  | false  |






### Returns pet inventories by status
Returns a map of status codes to quantities

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|



#### Output



Type: OBJECT







### Place an order for a pet
Place a new order in the store

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| order | Order | {INTEGER\(id), INTEGER\(petId), INTEGER\(quantity), DATE_TIME\(shipDate), STRING\(status), BOOLEAN\(complete)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| petId | Pet Id | INTEGER | INTEGER  |  | false  |
| quantity | Quantity | INTEGER | INTEGER  |  | false  |
| shipDate | Ship Date | DATE_TIME | DATE_TIME  |  | false  |
| status | Status | STRING | SELECT  |  Order Status  |  false  |
| complete | Complete | BOOLEAN | SELECT  |  | false  |






### Delete purchase order by ID
For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| orderId | Order Id | INTEGER | INTEGER  |  ID of the order that needs to be deleted  |  true  |




### Find purchase order by ID
For valid response try integer IDs with value <= 5 or > 10. Other values will generate exceptions.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| orderId | Order Id | INTEGER | INTEGER  |  ID of order that needs to be fetched  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| petId | Pet Id | INTEGER | INTEGER  |  | false  |
| quantity | Quantity | INTEGER | INTEGER  |  | false  |
| shipDate | Ship Date | DATE_TIME | DATE_TIME  |  | false  |
| status | Status | STRING | SELECT  |  Order Status  |  false  |
| complete | Complete | BOOLEAN | SELECT  |  | false  |






### Create user
This can only be done by the logged in user.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| user | User | {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| username | Username | STRING | TEXT  |  | false  |
| firstName | First Name | STRING | TEXT  |  | false  |
| lastName | Last Name | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| password | Password | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| userStatus | User Status | INTEGER | INTEGER  |  User Status  |  false  |






### Creates list of users with given input array
Creates list of users with given input array

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __items | Items | [{INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)}] | ARRAY_BUILDER  |  | null  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} | OBJECT_BUILDER  |






### Delete user
This can only be done by the logged in user.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  The name that needs to be deleted  |  true  |




### Get user by user name


#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  The name that needs to be fetched. Use user1 for testing.   |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| username | Username | STRING | TEXT  |  | false  |
| firstName | First Name | STRING | TEXT  |  | false  |
| lastName | Last Name | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| password | Password | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| userStatus | User Status | INTEGER | INTEGER  |  User Status  |  false  |






### Update user
This can only be done by the logged in user.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  name that need to be deleted  |  true  |
| user | User | {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | Id | INTEGER | INTEGER  |  | false  |
| username | Username | STRING | TEXT  |  | false  |
| firstName | First Name | STRING | TEXT  |  | false  |
| lastName | Last Name | STRING | TEXT  |  | false  |
| email | Email | STRING | TEXT  |  | false  |
| password | Password | STRING | TEXT  |  | false  |
| phone | Phone | STRING | TEXT  |  | false  |
| userStatus | User Status | INTEGER | INTEGER  |  User Status  |  false  |








## Triggers



<hr />

