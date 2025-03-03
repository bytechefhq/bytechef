---
title: "Trello"
description: "Trello is a project management tool that uses boards, lists, and cards to help users organize tasks and collaborate with teams."
---

Trello is a project management tool that uses boards, lists, and cards to help users organize tasks and collaborate with teams.


Categories: productivity-and-collaboration


Type: trello/v1

<hr />



## Connections

Version: 1


### custom

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING |  | true |
| token | Token | STRING |  | true |





<hr />



## Actions


### Create Board
Name: createBoard

Creates a new board.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The new name for the board. | true |
| desc | Description | STRING | A new description for the board. | false |


#### Output

This action does not produce any output.

#### JSON Example
```json
{
  "label" : "Create Board",
  "name" : "createBoard",
  "parameters" : {
    "name" : "",
    "desc" : ""
  },
  "type" : "trello/v1/createBoard"
}
```


### Create Card
Name: createCard

Creates a new card.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| idBoard | Board ID | STRING | ID of the board. | true |
| idList | List ID | STRING <details> <summary> Depends On </summary> idBoard </details> | ID of the list where the card should be created in. | true |
| name | Name | STRING | The name for the card. | false |
| desc | Description | STRING | The description for the card. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| desc | STRING |
| idBoard | STRING |
| idList | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "Create Card",
  "name" : "createCard",
  "parameters" : {
    "idBoard" : "",
    "idList" : "",
    "name" : "",
    "desc" : ""
  },
  "type" : "trello/v1/createCard"
}
```


### Get Card
Name: getCard

Gets a card details.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| idBoard | Board ID | STRING | ID of the board where card is located. | true |
| id | Card ID | STRING <details> <summary> Depends On </summary> idBoard </details> |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| desc | STRING |
| idBoard | STRING |
| idList | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "Get Card",
  "name" : "getCard",
  "parameters" : {
    "idBoard" : "",
    "id" : ""
  },
  "type" : "trello/v1/getCard"
}
```




## Triggers


### New Card
Name: newCard

Triggers when a new card is created on specified board or list.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| idBoard | Board ID | STRING |  | true |
| idList | List ID | STRING <details> <summary> Depends On </summary> idBoard </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| desc | STRING |
| idBoard | STRING |
| idList | STRING |
| name | STRING |




#### JSON Example
```json
{
  "label" : "New Card",
  "name" : "newCard",
  "parameters" : {
    "idBoard" : "",
    "idList" : ""
  },
  "type" : "trello/v1/newCard"
}
```


<hr />

