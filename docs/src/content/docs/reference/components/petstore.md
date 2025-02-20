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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |



### API Key

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| key | Key | STRING | TEXT |  | true |
| value | Value | STRING | TEXT |  | true |





<hr />



## Actions


### Add a new pet to the store
Name: addPet

Add a new pet to the store

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| pet | Pet | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} </details> | OBJECT_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| name | STRING | TEXT |
| category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| photoUrls | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> | ARRAY_BUILDER |
| status | STRING <details> <summary> Options </summary> available, pending, sold </details> | SELECT |




#### JSON Example
```json
{
  "label" : "Add a new pet to the store",
  "name" : "addPet",
  "parameters" : {
    "pet" : {
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
    }
  },
  "type" : "petstore/v1/addPet"
}
```


### Update an existing pet
Name: updatePet

Update an existing pet by Id

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| pet | Pet | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} </details> | OBJECT_BUILDER |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| name | STRING | TEXT |
| category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| photoUrls | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> | ARRAY_BUILDER |
| status | STRING <details> <summary> Options </summary> available, pending, sold </details> | SELECT |




#### JSON Example
```json
{
  "label" : "Update an existing pet",
  "name" : "updatePet",
  "parameters" : {
    "pet" : {
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
    }
  },
  "type" : "petstore/v1/updatePet"
}
```


### Finds Pets by status
Name: findPetsByStatus

Multiple status values can be provided with comma separated strings

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| status | Status | STRING <details> <summary> Options </summary> available, pending, sold </details> | SELECT | Status values that need to be considered for filter | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Tags to filter by | false |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name), {INTEGER\(id), STRING\(name)}\(category), [STRING]\(photoUrls), [{INTEGER\(id), STRING\(name)}]\(tags), STRING\(status)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| api_key | Api Key | STRING | TEXT |  | false |
| petId | Pet Id | INTEGER | INTEGER | Pet id to delete | true |


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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| petId | Pet Id | INTEGER | INTEGER | ID of pet to return | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| name | STRING | TEXT |
| category | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| photoUrls | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER |
| tags | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(name)}] </details> | ARRAY_BUILDER |
| status | STRING <details> <summary> Options </summary> available, pending, sold </details> | SELECT |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| petId | Pet Id | INTEGER | INTEGER | ID of pet that needs to be updated | true |
| name | Name | STRING | TEXT | Name of pet that needs to be updated | false |
| status | Status | STRING | TEXT | Status of pet that needs to be updated | false |


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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| petId | Pet Id | INTEGER | INTEGER | ID of pet to update | true |
| additionalMetadata | Additional Metadata | STRING | TEXT | Additional Metadata | false |
| fileEntry | | FILE_ENTRY | FILE_ENTRY |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| code | INTEGER | INTEGER |
| type | STRING | TEXT |
| message | STRING | TEXT |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| order | Order | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), INTEGER\(petId), INTEGER\(quantity), DATE_TIME\(shipDate), STRING\(status), BOOLEAN\(complete)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| petId | INTEGER | INTEGER |
| quantity | INTEGER | INTEGER |
| shipDate | DATE_TIME | DATE_TIME |
| status | STRING <details> <summary> Options </summary> placed, approved, delivered </details> | SELECT |
| complete | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |




#### JSON Example
```json
{
  "label" : "Place an order for a pet",
  "name" : "placeOrder",
  "parameters" : {
    "order" : {
      "id" : 1,
      "petId" : 1,
      "quantity" : 1,
      "shipDate" : "2021-01-01T00:00:00",
      "status" : "",
      "complete" : false
    }
  },
  "type" : "petstore/v1/placeOrder"
}
```


### Delete purchase order by ID
Name: deleteOrder

For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| orderId | Order Id | INTEGER | INTEGER | ID of the order that needs to be deleted | true |


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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| orderId | Order Id | INTEGER | INTEGER | ID of order that needs to be fetched | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| petId | INTEGER | INTEGER |
| quantity | INTEGER | INTEGER |
| shipDate | DATE_TIME | DATE_TIME |
| status | STRING <details> <summary> Options </summary> placed, approved, delivered </details> | SELECT |
| complete | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| user | User | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| username | STRING | TEXT |
| firstName | STRING | TEXT |
| lastName | STRING | TEXT |
| email | STRING | TEXT |
| password | STRING | TEXT |
| phone | STRING | TEXT |
| userStatus | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Create user",
  "name" : "createUser",
  "parameters" : {
    "user" : {
      "id" : 1,
      "username" : "",
      "firstName" : "",
      "lastName" : "",
      "email" : "",
      "password" : "",
      "phone" : "",
      "userStatus" : 1
    }
  },
  "type" : "petstore/v1/createUser"
}
```


### Creates list of users with given input array
Name: createUsersWithListInput

Creates list of users with given input array

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __items | Items | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)}] </details> | ARRAY_BUILDER |  | null |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
|  | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} </details> | OBJECT_BUILDER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| username | Username | STRING | TEXT | The name that needs to be deleted | true |


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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| username | Username | STRING | TEXT | The name that needs to be fetched. Use user1 for testing.  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| username | STRING | TEXT |
| firstName | STRING | TEXT |
| lastName | STRING | TEXT |
| email | STRING | TEXT |
| password | STRING | TEXT |
| phone | STRING | TEXT |
| userStatus | INTEGER | INTEGER |




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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| username | Username | STRING | TEXT | name that need to be deleted | true |
| user | User | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(username), STRING\(firstName), STRING\(lastName), STRING\(email), STRING\(password), STRING\(phone), INTEGER\(userStatus)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | INTEGER | INTEGER |
| username | STRING | TEXT |
| firstName | STRING | TEXT |
| lastName | STRING | TEXT |
| email | STRING | TEXT |
| password | STRING | TEXT |
| phone | STRING | TEXT |
| userStatus | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Update user",
  "name" : "updateUser",
  "parameters" : {
    "username" : "",
    "user" : {
      "id" : 1,
      "username" : "",
      "firstName" : "",
      "lastName" : "",
      "email" : "",
      "password" : "",
      "phone" : "",
      "userStatus" : 1
    }
  },
  "type" : "petstore/v1/updateUser"
}
```




