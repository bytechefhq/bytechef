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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |



### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING |  | true |
| value | Value | STRING |  | true |





<hr />



## Actions


### Add a new pet to the store
Name: addPet

Add a new pet to the store

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Id | INTEGER |  | false |
| name | Name | STRING |  | true |
| category | Category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> |  | false |
| photoUrls | Photo Urls | ARRAY <details> <summary> Items </summary> [STRING] </details> |  | true |
| tags | Tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> |  | false |
| status | Status | STRING <details> <summary> Options </summary> available, pending, sold </details> | pet status in the store | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| name | STRING |
| category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> |
| photoUrls | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> |
| status | STRING <details> <summary> Options </summary> available, pending, sold </details> |




#### JSON Example
```json
{
  "label" : "Add a new pet to the store",
  "name" : "addPet",
  "parameters" : {
    "id" : 1,
    "name" : "",
    "category" : {
      "id" : 1,
      "name" : ""
    },
    "photoUrls" : [ "" ],
    "tags" : [ {
      "id" : 1,
      "name" : ""
    } ],
    "status" : ""
  },
  "type" : "petstore/v1/addPet"
}
```


### Update an existing pet
Name: updatePet

Update an existing pet by Id

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Id | INTEGER |  | false |
| name | Name | STRING |  | true |
| category | Category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> |  | false |
| photoUrls | Photo Urls | ARRAY <details> <summary> Items </summary> [STRING] </details> |  | true |
| tags | Tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> |  | false |
| status | Status | STRING <details> <summary> Options </summary> available, pending, sold </details> | pet status in the store | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| name | STRING |
| category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> |
| photoUrls | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> |
| status | STRING <details> <summary> Options </summary> available, pending, sold </details> |




#### JSON Example
```json
{
  "label" : "Update an existing pet",
  "name" : "updatePet",
  "parameters" : {
    "id" : 1,
    "name" : "",
    "category" : {
      "id" : 1,
      "name" : ""
    },
    "photoUrls" : [ "" ],
    "tags" : [ {
      "id" : 1,
      "name" : ""
    } ],
    "status" : ""
  },
  "type" : "petstore/v1/updatePet"
}
```


### Finds Pets by status
Name: findPetsByStatus

Multiple status values can be provided with comma separated strings

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| status | Status | STRING <details> <summary> Options </summary> available, pending, sold </details> | Status values that need to be considered for filter | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} </details> |




#### JSON Example
```json
{
  "label" : "Finds Pets by status",
  "name" : "findPetsByStatus",
  "parameters" : {
    "status" : ""
  },
  "type" : "petstore/v1/findPetsByStatus"
}
```


### Finds Pets by tags
Name: findPetsByTags

Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | Tags to filter by | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} </details> |




#### JSON Example
```json
{
  "label" : "Finds Pets by tags",
  "name" : "findPetsByTags",
  "parameters" : {
    "tags" : [ "" ]
  },
  "type" : "petstore/v1/findPetsByTags"
}
```


### Deletes a pet
Name: deletePet

delete a pet

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| api_key | Api Key | STRING |  | false |
| petId | Pet Id | INTEGER | Pet id to delete | true |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Deletes a pet",
  "name" : "deletePet",
  "parameters" : {
    "api_key" : "",
    "petId" : 1
  },
  "type" : "petstore/v1/deletePet"
}
```


### Find pet by ID
Name: getPetById

Returns a single pet

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| petId | Pet Id | INTEGER | ID of pet to return | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| name | STRING |
| category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> |
| photoUrls | ARRAY <details> <summary> Items </summary> [STRING] </details> |
| tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> |
| status | STRING <details> <summary> Options </summary> available, pending, sold </details> |




#### JSON Example
```json
{
  "label" : "Find pet by ID",
  "name" : "getPetById",
  "parameters" : {
    "petId" : 1
  },
  "type" : "petstore/v1/getPetById"
}
```


### Updates a pet in the store with form data
Name: updatePetWithForm



#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| petId | Pet Id | INTEGER | ID of pet that needs to be updated | true |
| name | Name | STRING | Name of pet that needs to be updated | false |
| status | Status | STRING | Status of pet that needs to be updated | false |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Updates a pet in the store with form data",
  "name" : "updatePetWithForm",
  "parameters" : {
    "petId" : 1,
    "name" : "",
    "status" : ""
  },
  "type" : "petstore/v1/updatePetWithForm"
}
```


### uploads an image
Name: uploadFile



#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| petId | Pet Id | INTEGER | ID of pet to update | true |
| additionalMetadata | Additional Metadata | STRING | Additional Metadata | false |
| fileEntry | | FILE_ENTRY |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| code | INTEGER |
| type | STRING |
| message | STRING |




#### JSON Example
```json
{
  "label" : "uploads an image",
  "name" : "uploadFile",
  "parameters" : {
    "petId" : 1,
    "additionalMetadata" : "",
    "fileEntry" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "petstore/v1/uploadFile"
}
```


### Returns pet inventories by status
Name: getInventory

Returns a map of status codes to quantities


#### Output



Type: OBJECT





#### JSON Example
```json
{
  "label" : "Returns pet inventories by status",
  "name" : "getInventory",
  "type" : "petstore/v1/getInventory"
}
```


### Place an order for a pet
Name: placeOrder

Place a new order in the store

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Id | INTEGER |  | false |
| petId | Pet Id | INTEGER |  | false |
| quantity | Quantity | INTEGER |  | false |
| shipDate | Ship Date | DATE_TIME |  | false |
| status | Status | STRING <details> <summary> Options </summary> placed, approved, delivered </details> | Order Status | false |
| complete | Complete | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| petId | INTEGER |
| quantity | INTEGER |
| shipDate | DATE_TIME |
| status | STRING <details> <summary> Options </summary> placed, approved, delivered </details> |
| complete | BOOLEAN <details> <summary> Options </summary> true, false </details> |




#### JSON Example
```json
{
  "label" : "Place an order for a pet",
  "name" : "placeOrder",
  "parameters" : {
    "id" : 1,
    "petId" : 1,
    "quantity" : 1,
    "shipDate" : "2021-01-01T00:00:00",
    "status" : "",
    "complete" : false
  },
  "type" : "petstore/v1/placeOrder"
}
```


### Delete purchase order by ID
Name: deleteOrder

For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| orderId | Order Id | INTEGER | ID of the order that needs to be deleted | true |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Delete purchase order by ID",
  "name" : "deleteOrder",
  "parameters" : {
    "orderId" : 1
  },
  "type" : "petstore/v1/deleteOrder"
}
```


### Find purchase order by ID
Name: getOrderById

For valid response try integer IDs with value <= 5 or > 10. Other values will generate exceptions.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| orderId | Order Id | INTEGER | ID of order that needs to be fetched | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| petId | INTEGER |
| quantity | INTEGER |
| shipDate | DATE_TIME |
| status | STRING <details> <summary> Options </summary> placed, approved, delivered </details> |
| complete | BOOLEAN <details> <summary> Options </summary> true, false </details> |




#### JSON Example
```json
{
  "label" : "Find purchase order by ID",
  "name" : "getOrderById",
  "parameters" : {
    "orderId" : 1
  },
  "type" : "petstore/v1/getOrderById"
}
```


### Create user
Name: createUser

This can only be done by the logged in user.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Id | INTEGER |  | false |
| username | Username | STRING |  | false |
| firstName | First Name | STRING |  | false |
| lastName | Last Name | STRING |  | false |
| email | Email | STRING |  | false |
| password | Password | STRING |  | false |
| phone | Phone | STRING |  | false |
| userStatus | User Status | INTEGER | User Status | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| username | STRING |
| firstName | STRING |
| lastName | STRING |
| email | STRING |
| password | STRING |
| phone | STRING |
| userStatus | INTEGER |




#### JSON Example
```json
{
  "label" : "Create user",
  "name" : "createUser",
  "parameters" : {
    "id" : 1,
    "username" : "",
    "firstName" : "",
    "lastName" : "",
    "email" : "",
    "password" : "",
    "phone" : "",
    "userStatus" : 1
  },
  "type" : "petstore/v1/createUser"
}
```


### Creates list of users with given input array
Name: createUsersWithListInput

Creates list of users with given input array

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| __items | Items | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)}] </details> |  | null |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} </details> |




#### JSON Example
```json
{
  "label" : "Creates list of users with given input array",
  "name" : "createUsersWithListInput",
  "parameters" : {
    "__items" : [ {
      "id" : 1,
      "username" : "",
      "firstName" : "",
      "lastName" : "",
      "email" : "",
      "password" : "",
      "phone" : "",
      "userStatus" : 1
    } ]
  },
  "type" : "petstore/v1/createUsersWithListInput"
}
```


### Delete user
Name: deleteUser

This can only be done by the logged in user.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING | The name that needs to be deleted | true |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Delete user",
  "name" : "deleteUser",
  "parameters" : {
    "username" : ""
  },
  "type" : "petstore/v1/deleteUser"
}
```


### Get user by user name
Name: getUserByName



#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING | The name that needs to be fetched. Use user1 for testing.  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| username | STRING |
| firstName | STRING |
| lastName | STRING |
| email | STRING |
| password | STRING |
| phone | STRING |
| userStatus | INTEGER |




#### JSON Example
```json
{
  "label" : "Get user by user name",
  "name" : "getUserByName",
  "parameters" : {
    "username" : ""
  },
  "type" : "petstore/v1/getUserByName"
}
```


### Update user
Name: updateUser

This can only be done by the logged in user.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING | name that need to be deleted | true |
| id | Id | INTEGER |  | false |
| username | Username | STRING |  | false |
| firstName | First Name | STRING |  | false |
| lastName | Last Name | STRING |  | false |
| email | Email | STRING |  | false |
| password | Password | STRING |  | false |
| phone | Phone | STRING |  | false |
| userStatus | User Status | INTEGER | User Status | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | INTEGER |
| username | STRING |
| firstName | STRING |
| lastName | STRING |
| email | STRING |
| password | STRING |
| phone | STRING |
| userStatus | INTEGER |




#### JSON Example
```json
{
  "label" : "Update user",
  "name" : "updateUser",
  "parameters" : {
    "username" : "",
    "id" : 1,
    "firstName" : "",
    "lastName" : "",
    "email" : "",
    "password" : "",
    "phone" : "",
    "userStatus" : 1
  },
  "type" : "petstore/v1/updateUser"
}
```




